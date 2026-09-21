# Version-code helper
- script/ is a standalone TypeScript/pnpm package. updateVersionCode.ts counts git commits with git rev-list --count HEAD and replaces the numeric versionCode in ./app/build.gradle.kts.
- The script resolves its target relative to process working directory, not __dirname. Compile from script/, execute from repository root: pnpm --dir script run build; node script/updateVersionCode.js.
- package.json's replace script runs node from the package directory, so its relative ./app/build.gradle.kts target is wrong in that context. Prefer the explicit two-step command above.
- Version rewriting is a deliberate source mutation for versioning/release work, not a validation step. Full Git history is needed for a meaningful commit count; script CI uses fetch-depth: 0.
- tsconfig.json uses strict TypeScript, ES2020 target, CommonJS, Node types. build runs tsc -p tsconfig.json. Generated updateVersionCode.js and node_modules are ignored.
- .github/workflows/script_build.yml installs with pnpm, compiles and uploads the JavaScript. It does not execute the version updater. There are no script test/lint commands.