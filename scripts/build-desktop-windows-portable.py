#!/usr/bin/env python3
"""Cross-build a self-contained Windows x64 ZIP with Go, JVM and .NET SDKs.

Native MSI/MSIX installation and Windows UI testing still require Windows.
This portable bundle does not register the Windows Widgets provider.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import tempfile
from urllib.request import urlopen
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'build/desktop-windows-portable'
JRE_URL = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jre_x64_windows_hotspot_21.0.12.1_1.zip'
JRE_SHA256 = 'd35f31e712f0fcf6ac5a093edc90204fbff22f720ba3950bd09d331d5e621636'


def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda: source.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def inputs():
    files = []
    for directory in ('desktopApp', 'desktopIntegration', 'shared', 'desktop-helper', 'sync-engine', 'native/windows', 'gradle'):
        files.extend(p for p in (ROOT / directory).rglob('*') if p.is_file()
                     and not set(p.relative_to(ROOT / directory).parts) & {'build', 'bin', 'obj', '.gradle'}
                     and p.suffix in {'.kt', '.kts', '.properties', '.go', '.mod', '.sum', '.cs', '.csproj', '.json', '.toml', '.appxmanifest'})
    files.extend(ROOT.glob('*.kts'))
    files.append(Path(__file__))
    return {str(p.relative_to(ROOT)): sha(p) for p in sorted(set(files))}


def run(*args, cwd=ROOT, env=None):
    subprocess.run([str(a) for a in args], cwd=cwd, env=env, check=True)


def verify_pe(path):
    with path.open('rb') as stream:
        assert stream.read(2) == b'MZ', path
        stream.seek(0x3c)
        stream.seek(struct.unpack('<I', stream.read(4))[0])
        assert stream.read(6) == b'PE\0\0\x64\x86', path


def main():
    before = inputs()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='windows-', dir=OUTPUT) as temporary:
        stage = Path(temporary)
        bundle = stage / 'VerveTwoDo'
        libs = bundle / 'app'
        resources = libs / 'resources'
        resources.mkdir(parents=True)
        init = stage / 'windows.gradle'
        init.write_text("""
gradle.projectsEvaluated {
    def app = rootProject.project(':desktopApp')
    app.tasks.register('exportPortableWindows') {
        dependsOn app.tasks.named('jar')
        doLast {
            def runtime = app.configurations.runtimeClasspath
            def skiko = runtime.resolvedConfiguration.resolvedArtifacts.find {
                it.moduleVersion.id.group == 'org.jetbrains.skiko' && it.name == 'skiko-awt'
            }
            if (skiko == null) throw new GradleException('Missing Skiko dependency')
            def windows = app.configurations.detachedConfiguration(app.dependencies.create(
                'org.jetbrains.skiko:skiko-awt-runtime-windows-x64:' + skiko.moduleVersion.id.version))
            app.copy {
                duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
                eachFile { entry ->
                    if (entry.name.endsWith('.jar')) {
                        def hash = java.security.MessageDigest.getInstance('SHA-256').digest(entry.file.bytes).encodeHex().toString().take(16)
                        entry.name = entry.name.replace('.jar', '-' + hash + '.jar')
                    }
                }
                from runtime
                from app.tasks.named('jar')
                from windows
                exclude '*runtime-macos-*', '*runtime-linux-*'
                into System.getenv('VERVE_WINDOWS_LIBS')
            }
        }
    }
}
""")
        run(ROOT / 'gradlew', '-PdesktopOnly=true', '-I', init,
            ':desktopIntegration:test', ':desktopApp:exportPortableWindows', '--console=plain',
            env=dict(os.environ, VERVE_WINDOWS_LIBS=str(libs)))
        assert list(libs.glob('skiko-awt-runtime-windows-x64-*.jar'))
        run('go', 'test', './...', cwd=ROOT / 'desktop-helper',
            env=dict(os.environ, GOWORK='off', GOPRIVATE='github.com/CamilleHbp/sync-core'))
        run('go', 'build', '-trimpath', '-o', resources / 'vervetwodo-helper.exe', '.',
            cwd=ROOT / 'desktop-helper', env=dict(os.environ, GOOS='windows', GOARCH='amd64', CGO_ENABLED='0', GOWORK='off', GOPRIVATE='github.com/CamilleHbp/sync-core'))
        # This project has one SDK-generated manifest and no user manifest to merge.
        # Keep the generated registration XML intact and let the C# compiler embed it.
        manifest = stage / 'single-manifest.py'
        manifest.write_text("#!/usr/bin/env python3\nimport sys, shutil, xml.etree.ElementTree as ET\na=sys.argv[1:]\nassert a[:2] == ['-nologo','-manifest'] and len(a)==4 and a[3].startswith('-out:'), a\nET.parse(a[2])\nshutil.copyfile(a[2],a[3][5:])\n")
        manifest.chmod(0o755)
        native = resources / 'VerveWidgets'
        # No XAML or app PRI assets: retain the SDK's prebuilt PRI payload as-is.
        run('dotnet', 'publish', ROOT / 'native/windows/VerveWidgets/VerveWidgets.csproj',
            '-c', 'Release', '-r', 'win-x64', '-p:ManifestTool=' + str(manifest),
            '-p:AppxGeneratePriEnabled=false', '-o', native)
        assert (native / 'Microsoft.UI.pri').is_file()
        assert (native / 'Microsoft.WindowsAppRuntime.dll').is_file()
        launcher = stage / 'launcher.go'
        launcher.write_text(r"""package main
import ("os"; "os/exec"; "path/filepath"; "syscall"; "unsafe")
func main() {
    executable, err := os.Executable()
    if err != nil { panic(err) }
    root := filepath.Dir(executable)
    args := []string{"-Dcompose.application.resources.dir=" + filepath.Join(root,"app","resources"), "-Dcompose.application.configure.swing.globals=true", "-Djpackage.app-version=1.0.0", "-cp", filepath.Join(root,"app","*"), "studio.camille.vervetwodo.desktop.MainKt"}
    args = append(args, os.Args[1:]...)
    command := exec.Command(filepath.Join(root,"runtime","bin","javaw.exe"),args...)
    command.Dir = root
    if err = command.Start(); err != nil {
        message, _ := syscall.UTF16PtrFromString("VerveTwoDo could not start. Extract the entire ZIP before opening the app.\n" + err.Error())
        title, _ := syscall.UTF16PtrFromString("VerveTwoDo")
        syscall.NewLazyDLL("user32.dll").NewProc("MessageBoxW").Call(0,uintptr(unsafe.Pointer(message)),uintptr(unsafe.Pointer(title)),0x10)
        os.Exit(1)
    }
    command.Process.Release()
}
""")
        run('go', 'build', '-trimpath', '-ldflags=-H=windowsgui', '-o', bundle / 'VerveTwoDo.exe', launcher,
            env=dict(os.environ, GOOS='windows', GOARCH='amd64', CGO_ENABLED='0', GOWORK='off'))
        archive = OUTPUT / 'temurin-windows-jre.zip'
        if not archive.exists() or sha(archive) != JRE_SHA256:
            with urlopen(JRE_URL, timeout=120) as source, archive.open('wb') as destination:
                shutil.copyfileobj(source, destination)
        if sha(archive) != JRE_SHA256:
            raise ValueError('Windows JRE checksum mismatch')
        with zipfile.ZipFile(archive) as source:
            source.extractall(stage / 'jre')
        runtime = next((stage / 'jre').iterdir())
        shutil.move(runtime, bundle / 'runtime')
        for relative in ('VerveTwoDo.exe', 'runtime/bin/javaw.exe', 'app/resources/vervetwodo-helper.exe', 'app/resources/VerveWidgets/VerveWidgets.exe'):
            verify_pe(bundle / relative)
        (bundle / 'README.txt').write_text('VerveTwoDo for Windows 11 x64\n\nExtract the whole ZIP, then open VerveTwoDo.exe. Keep the app, runtime and resources folders together. Java and .NET are included. Your tasks stay in your Windows user profile.\n\nThis portable download does not register a Windows Widgets provider.\n\nVerveTwoDo pour Windows 11 x64\n\nExtrayez le ZIP complet, puis ouvrez VerveTwoDo.exe. Conservez ensemble les dossiers app, runtime et resources. Java et .NET sont inclus. Vos tâches restent dans votre profil Windows.\n\nCe téléchargement portable n’enregistre pas de fournisseur de widgets Windows.\n', encoding='utf-8')
        # Runtime notices stay with the bundled runtimes; include the app licence too.
        for licence in ROOT.glob('LICENSE*'):
            if licence.is_file(): shutil.copy2(licence, bundle / licence.name)
        if inputs() != before:
            raise RuntimeError('Desktop build inputs changed during packaging; rebuild before publication')
        result = OUTPUT / 'VerveTwoDo-1.0.0-windows-x64.zip'
        with zipfile.ZipFile(result, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as destination:
            for path in sorted(bundle.rglob('*')):
                if path.is_file() and path.suffix != '.pdb':
                    destination.write(path, Path('VerveTwoDo') / path.relative_to(bundle))
        with zipfile.ZipFile(result) as check:
            assert check.testzip() is None
        (OUTPUT / 'receipt.json').write_text(json.dumps({'inputs': before, 'artifact': str(result), 'sha256': sha(result), 'size': result.stat().st_size, 'validation': 'SDK cross-build, desktop unit tests, PE architecture, runtime and ZIP integrity; Windows runtime testing not performed'}, indent=2) + '\n')
        print(result)
        print(sha(result))


if __name__ == '__main__':
    main()
