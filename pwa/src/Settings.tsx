import { useEffect, useRef, useState } from "react";
import {
  Data,
  backup,
  csv,
  importBackup,
  today,
  conflicts,
  resolve,
  value,
} from "./model";
import { IconLabel } from "./TaskIcon";
import { Text } from "./i18n";
import { Modal, Field, TimeInput } from "./components";
import { connect, disconnect, sync, SyncStatus } from "./sync";
import { enablePush, disablePush } from "./push";
import {
  WidgetPlatform,
  widgetDownloads,
  widgetInstallCommand,
  widgetInstalled,
  widgetPlatform,
} from "./platform";
type Props = {
  data: Data;
  t: Text;
  action: (fn: (d: Data) => void, message?: string) => void;
  notice: (s: string) => void;
  onClose: () => void;
};
export function Settings({
  data,
  t,
  action,
  notice,
  onClose,
  onSync,
  confirm,
}: Props & {
  onSync: () => void;
  confirm: (c: { message: string; action: () => void }) => void;
}) {
  const prefs = data.preferences,
    importRef = useRef<HTMLInputElement>(null);
  const [prompt, setPrompt] = useState<any>(null);
  const [widget, setWidget] = useState<WidgetPlatform | null>(null);
  useEffect(() => {
    void widgetPlatform().then(setWidget);
  }, []);
  useEffect(() => {
    const handler = (e: Event) => {
      e.preventDefault();
      setPrompt(e);
    };
    window.addEventListener("beforeinstallprompt", handler);
    return () => window.removeEventListener("beforeinstallprompt", handler);
  }, []);
  const download = (contents: string, ext: string, type: string) => {
    const url = URL.createObjectURL(new Blob([contents], { type })),
      a = document.createElement("a");
    a.href = url;
    a.download = `vervetwodo-${today()}.${ext}`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 10000);
  };
  return (
    <Modal
      title={t("settings")}
      onClose={onClose}
      footer={
        <button className="primary" onClick={onClose}>
          {t("done")}
        </button>
      }
    >
      {[
        ["language", ["system", "en", "fr"]],
        ["theme", ["system", "light", "dark"]],
      ].map(([key, options]) => (
        <Field
          key={String(key)}
          label={t(key === "theme" ? "appearance" : String(key))}
        >
          <select
            value={prefs[key as "language" | "theme"]}
            onChange={(e) => {
              const v = e.target.value;
              action((d) => {
                d.preferences[key as "language" | "theme"] = v;
              });
            }}
          >
            {(options as string[]).map((v) => (
              <option value={v} key={v}>
                {t(v)}
              </option>
            ))}
          </select>
        </Field>
      ))}
      <label className="schedule-toggle rollover-setting"><IconLabel name="rollover" off={!prefs.rolloverDefault}>{t("rolloverDefault")}</IconLabel>
        <input
          type="checkbox"
          checked={prefs.rolloverDefault}
          onChange={(e) => {
            const checked = e.target.checked;
            action((d) => {
              d.preferences.rolloverDefault = checked;
            });
          }}
        />
      </label>
      <p className="hint">{t("rolloverDefaultHint")}</p>
      <h3>{t("taskReminders")}</h3>
      <p className="hint">{t("webRemindersHint")}</p>
      <label className="toggle">
        <input
          type="checkbox"
          checked={prefs.reminders}
          onChange={async (e) => {
            const checked = e.target.checked;
            if (!checked && prefs.push) {
              try {
                await disablePush();
              } catch {
                notice("pushUpdateError");
                return;
              }
            }
            if (
              checked &&
              (!("Notification" in window) ||
                (await Notification.requestPermission()) !== "granted")
            ) {
              notice("notificationError");
              return;
            }
            action((d) => {
              d.preferences.reminders = checked;
            });
          }}
        />
        {t("taskReminders")}
      </label>
      {data.connected && (
        <button
          onClick={async () => {
            try {
              if (prefs.push) await disablePush();
              else await enablePush(data);
            } catch {
              notice("pushUnavailable");
            }
          }}
        >
          {t(prefs.push ? "disablePush" : "enablePush")}
        </button>
      )}
      {prefs.push && <p className="hint">{t("pushEnabled")}</p>}
      <TimeInput
        label={t("defaultReminderTime")}
        value={prefs.reminderTime}
        fallback="09:00"
        optional={false}
        t={t}
        onChange={(v) =>
          action((d) => {
            d.preferences.reminderTime = v;
          })
        }
      />
      <Field label={t("advanceMinutes")}>
        <input
          type="number"
          min="0"
          max="10080"
          value={prefs.advance}
          onChange={(e) => {
            const v = Math.max(0, Math.min(10080, Number(e.target.value)));
            action((d) => {
              d.preferences.advance = v;
            });
          }}
        />
      </Field>
      <button
        onClick={async () => {
          try {
            if (
              !("Notification" in window) ||
              (await Notification.requestPermission()) !== "granted"
            )
              throw Error();
            const reg = await navigator.serviceWorker.getRegistration();
            if (!reg) throw Error();
            await reg.showNotification(t("notificationTitle"), {
              body: t("notificationBody"),
              icon: "/icons/app-192.png",
            });
            notice("notificationSent");
          } catch {
            notice("notificationError");
          }
        }}
      >
        {t("notification")}
      </button>
      <h3>{t("data")}</h3>
      <div className="settings-actions">
        <button
          onClick={() => download(backup(data), "json", "application/json")}
        >
          {t("backup")}
        </button>
        <button
          onClick={() => download(csv(data), "csv", "text/csv;charset=utf-8")}
        >
          {t("exportCsv")}
        </button>
        <button onClick={() => importRef.current?.click()}>
          {t("importBackup")}
        </button>
        <button onClick={onSync}>{t("syncTitle")}</button>
        <button
          onClick={async () => {
            notice(
              (await navigator.storage?.persist?.())
                ? "storagePersistent"
                : "storageBestEffort",
            );
          }}
        >
          {t("persistentStorage")}
        </button>
      </div>
      <input
        hidden
        ref={importRef}
        type="file"
        accept=".json,application/json"
        onChange={async (e) => {
          const file = e.target.files?.[0];
          e.target.value = "";
          if (!file) return;
          if (file.size > 3 * 1024 * 1024) {
            notice("invalidBackup");
            return;
          }
          const text = await file.text();
          confirm({
            message: "backupImportConfirm",
            action: () => action((d) => importBackup(d, text), "saved"),
          });
        }}
      />
      {widget && (
        <>
          <h3>{t("widgetTitle")}</h3>
          {widgetInstalled() ? (
            <p className="hint">{t("widgetInstalled")}</p>
          ) : (
            <>
              <p className="hint">{t("widgetHint")}</p>
              {widget === "macos" && (
                <>
                  <p className="hint">{t("widgetMacCommand")}</p>
                  <pre className="command">
                    <code>{widgetInstallCommand}</code>
                  </pre>
                  <div className="settings-actions">
                    <button
                      onClick={async () => {
                        try {
                          await navigator.clipboard.writeText(widgetInstallCommand);
                          notice("widgetCopied");
                        } catch {
                          notice("widgetCopyFailed");
                        }
                      }}
                    >
                      {t("widgetCopy")}
                    </button>
                  </div>
                  <p className="hint">{t("widgetMacDownloadIntro")}</p>
                </>
              )}
              <ol className="hint">
                {(widget === "macos"
                  ? ["widgetMacStep1", "widgetMacStep2", "widgetMacStep3"]
                  : ["widgetWindowsStep1", "widgetWindowsStep2", "widgetWindowsStep3"]
                ).map((key) => (
                  <li key={key}>{t(key)}</li>
                ))}
              </ol>
            </>
          )}
          <div className="settings-actions">
            <a className="button" href={widgetDownloads[widget]}>
              {t(widget === "macos" ? "widgetDownloadMac" : "widgetDownloadWindows")}
            </a>
          </div>
        </>
      )}
      <h3>{t("about")}</h3>
      <p>VerveTwoDo Web · 1.0.0</p>
      <p className="hint">{t("offlineReady")}</p>
      <div className="settings-actions">
        <button
          onClick={async () => {
            if (prompt) {
              await prompt.prompt();
              setPrompt(null);
            } else notice("installHint");
          }}
        >
          {t("install")}
        </button>
        <button
          onClick={async () => {
            try {
              const reg = await navigator.serviceWorker.getRegistration();
              if (!reg) throw Error();
              await reg.update();
              notice(reg.waiting ? "updateReady" : "upToDate");
            } catch {
              notice("checkUpdatesError");
            }
          }}
        >
          {t("checkUpdates")}
        </button>
      </div>
      <p className="hint">{t("keyboard")}</p>
      <p className="hint">
        <a href="/LICENSE.txt" target="_blank" rel="noreferrer">
          GPL-3.0
        </a>{" "}
        · VerveDo / super12138 · Camille Studio
      </p>
    </Modal>
  );
}
export function SyncSettings({
  data,
  t,
  action,
  notice,
  onClose,
  status,
}: Props & { status: SyncStatus }) {
  const [code, setCode] = useState(""),
    [busy, setBusy] = useState(false);
  const issues = conflicts(data);
  return (
    <Modal
      title={t("syncTitle")}
      onClose={onClose}
      footer={<button onClick={onClose}>{t("close")}</button>}
    >
      <p>{t("syncWebDescription")}</p>
      <p className="sync-state">{t(status)}</p>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          try {
            await connect(code);
            setCode("");
          } catch (e) {
            notice(e instanceof Error ? e.message : "syncError");
          } finally {
            setBusy(false);
          }
        }}
      >
        <Field label={t("syncCode")}>
          <input
            type="password"
            autoComplete="off"
            required
            value={code}
            onChange={(e) => setCode(e.target.value)}
          />
        </Field>
        <button className="primary" disabled={busy}>
          {t("syncConnect")}
        </button>
      </form>
      <p className="hint">{t("syncPrivacy")}</p>
      {data.connected && (
        <div className="settings-actions">
          <button disabled={busy} onClick={() => void sync()}>
            {t("syncNow")}
          </button>
          <button
            onClick={() => void disconnect().catch(() => notice("syncError"))}
          >
            {t("disconnect")}
          </button>
        </div>
      )}
      <p className="hint">{t("disconnectHint")}</p>
      {issues.length > 0 && (
        <>
          <h3>{t("syncAttentionTitle")}</h3>
          <p>{t("syncAttentionDescription")}</p>
          {issues.map((issue) => (
            <section
              className="conflict"
              key={`${issue.record.id}:${issue.field}`}
            >
              <h4>
                {value(issue.record, "title") || value(issue.record, "name")} ·{" "}
                {t(`syncField_${issue.field}`)}
              </h4>
              {issue.versions.map((v) => (
                <div key={v.id}>
                  <pre>{v.value || t("syncEmpty")}</pre>
                  <p className="hint">
                    {v.source?.name || t("syncUnknownDevice")}
                  </p>
                  <button
                    onClick={() =>
                      action((d) =>
                        resolve(
                          d,
                          issue.record.id,
                          issue.field,
                          v.value,
                          issue.versions.map((v) => v.id),
                        ),
                      )
                    }
                  >
                    {t("syncKeep")}
                  </button>
                </div>
              ))}
            </section>
          ))}
        </>
      )}
    </Modal>
  );
}
