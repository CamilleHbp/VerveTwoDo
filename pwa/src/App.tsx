import React, { useEffect, useRef, useState } from "react";
import {
  Data,
  Task,
  Tag,
  commit,
  completion,
  undo,
  newTask,
  equal,
  sortTasks,
  groupKey,
  today,
  conflicts,
  rollover,
} from "./model";
import { mutate, read, subscribe } from "./storage";
import { translator, locale } from "./i18n";
import {
  Artwork,
  Modal,
  Field,
  TagChoices,
  TaskModal,
  TagModal,
  BulkModal,
} from "./components";
import { Settings, SyncSettings } from "./Settings";
import { sync, onSync, startSync, SyncStatus } from "./sync";
import { remind } from "./reminders";
import { updatePush } from "./push";
import {
  WidgetPlatform,
  widgetDownloads,
  widgetLink,
  type WidgetLink,
  widgetInstalled,
  widgetOfferDismissed,
  widgetPlatform,
} from "./platform";
import "./style.css";
import { TaskIcon } from "./TaskIcon";
export default function App() {
  const [data, setData] = useState<Data>(),
    [fatal, setFatal] = useState(""),
    [view, setView] = useState("agenda"),
    [tagFilter, setTagFilter] = useState<string | null>(null),
    [search, setSearch] = useState(""),
    [status, setStatus] = useState("open"),
    [sort, setSort] = useState("due"),
    [selected, setSelected] = useState<string[]>([]),
    [selecting, setSelecting] = useState(false),
    [collapsed, setCollapsed] = useState<string[]>([]),
    [task, setTask] = useState<Task>(),
    [tag, setTag] = useState<Tag | null | undefined>(),
    [panel, setPanel] = useState(""),
    [bulk, setBulk] = useState(false),
    [notice, setNotice] = useState(""),
    [syncStatus, setSyncStatus] = useState<SyncStatus>("local"),
    [update, setUpdate] = useState(false),
    [confirm, setConfirm] = useState<{ message: string; action: () => void }>(),
    [widget, setWidget] = useState<WidgetPlatform | null>(null),
    [widgetOffer, setWidgetOffer] = useState(
      () => !widgetInstalled() && !widgetOfferDismissed(),
    );
  const searchRef = useRef<HTMLInputElement>(null);
  const t = translator(data?.preferences.language || "system");
  useEffect(() => {
    if (data?.connected && data.preferences.push) {
      void updatePush(data).catch(() => setNotice("pushUpdateError"));
    }
  }, [
    data?.connected,
    data?.preferences.push,
    data?.preferences.language,
    data?.preferences.reminderTime,
    data?.preferences.advance,
  ]);
  useEffect(() => {
    let live = true;
    const load = () =>
      read()
        .then((d) => {
          if (live) setData((prior) => prior && prior.revision > d.revision ? prior : d);
        })
        .catch(() => setFatal("storageError"));
    void mutate(() => {})
      .then(load)
      .catch(() => setFatal("storageError"));
    const stop = subscribe(() => void load());
    onSync(setSyncStatus);
    const stopSync = startSync();
    return () => {
      live = false;
      stop();
      stopSync();
    };
  }, []);
  useEffect(() => {
    document.documentElement.lang = locale(
      data?.preferences.language || "system",
    );
    document.documentElement.dataset.theme =
      data?.preferences.theme || "system";
  }, [data?.preferences.language, data?.preferences.theme]);
  useEffect(() => {
    const key = (e: KeyboardEvent) => {
      if (e.metaKey || e.ctrlKey) {
        if (
          e.key.toLowerCase() === "n" &&
          !task &&
          !panel &&
          tag === undefined
        ) {
          e.preventDefault();
          setTask(newTask(data?.preferences.rolloverDefault));
        } else if (e.key.toLowerCase() === "f" && !task && !panel) {
          e.preventDefault();
          searchRef.current?.focus();
        }
      }
    };
    window.addEventListener("keydown", key);
    return () => window.removeEventListener("keydown", key);
  }, [task, panel, tag, data?.preferences.rolloverDefault]);
  useEffect(() => {
    if (!data) return;
    const pulse = async () => {
      await mutate(rollover);
      await remind(await read(), t("reminderBody"));
    };
    void pulse().catch(() => {});
    const timer = setInterval(() => void pulse().catch(() => {}), 30000);
    return () => clearInterval(timer);
  }, [
    !!data,
    data?.preferences.reminders,
    data?.preferences.reminderTime,
    data?.preferences.advance,
  ]);
  // Widget links arrive in the page URL on launch, or through the launch queue when the
  // installed app is already open (launch_handler: focus-existing).
  const [pendingLink, setPendingLink] = useState<WidgetLink | null>(() =>
    widgetLink(location.search),
  );
  useEffect(() => {
    (window as any).launchQueue?.setConsumer(
      (params: { targetURL?: string }) => {
        if (!params.targetURL) return;
        const link = widgetLink(new URL(params.targetURL).search);
        if (link) setPendingLink(link);
      },
    );
  }, []);
  useEffect(() => {
    if (!data || !pendingLink) return;
    const link = pendingLink;
    setPendingLink(null);
    // Links from the desktop widget companion identify it as installed.
    if (link.fromWidget) {
      widgetInstalled(true);
      setWidgetOffer(false);
    }
    if (link.task) {
      const item = data.tasks.find((t) => t.id === link.task && !t.deleted);
      if (item) setTask(item);
    } else if (link.add) {
      setTask(newTask(data.preferences.rolloverDefault));
    }
    if (location.search) history.replaceState({}, "", location.pathname);
  }, [!!data, pendingLink]);
  useEffect(() => {
    void widgetPlatform().then(setWidget);
  }, []);
  useEffect(() => {
    if (!("serviceWorker" in navigator)) return;
    const message = (e: MessageEvent) => {
      if (e.data?.taskId)
        void read().then((d) => {
          const item = d.tasks.find((t) => t.id === e.data.taskId);
          if (item) setTask(item);
        });
    };
    navigator.serviceWorker.addEventListener("message", message);
    void navigator.serviceWorker
      .register("/sw.js")
      .then((reg) => {
        if (reg.waiting) setUpdate(true);
        reg.addEventListener("updatefound", () => {
          const installing = reg.installing;
          installing?.addEventListener("statechange", () => {
            if (
              installing.state === "installed" &&
              navigator.serviceWorker.controller
            )
              setUpdate(true);
          });
        });
      })
      .catch(() => {});
    return () =>
      navigator.serviceWorker.removeEventListener("message", message);
  }, []);
  const run = async (fn: (d: Data) => void, message = "") => {
    try {
      const updated = await mutate(fn);
      if (message) setNotice(message);
      void sync();
      return updated;
    } catch (e) {
      setNotice(e instanceof Error ? e.message : "error");
      throw e;
    }
  };
  const action = (fn: (d: Data) => void, message = "") => {
    void run(fn, message).catch(() => {});
  };
  const remove = (ids: string[]) =>
    setConfirm({
      message: "deleteBody",
      action: () => {
        action(
          (d) =>
            commit(
              d,
              d.tasks
                .filter((t) => ids.includes(t.id))
                .map((t) => ({ ...t, deleted: true })),
            ),
          "saved",
        );
        setTask(undefined);
        setSelected([]);
      },
    });
  const toggle = (id: string, done: boolean) =>
    action((d) => commit(d, completion(d, [id], done)));
  const save = async (draft: Task, base: Task) => {
    const updated = await run((d) => {
      const current = d.tasks.find((t) => t.id === draft.id);
      if ((current && !equal(current, base)) || (!current && base.createdAt))
        throw Error("taskChanged");
      commit(d, [draft]);
    }, "saved");
    setData((prior) => prior && prior.revision > updated.revision ? prior : updated);
    return updated.tasks.find((task) => task.id === draft.id)!;
  };
  if (fatal)
    return (
      <main className="fatal">
        <h1>VerveTwoDo</h1>
        <p role="alert">{t(fatal)}</p>
        <button onClick={() => location.reload()}>{t("retry")}</button>
      </main>
    );
  if (!data)
    return (
      <main className="fatal" aria-busy="true">
        {t("loading")}
      </main>
    );
  const prefs = data.preferences,
    live = data.tasks.filter((t) => !t.deleted),
    issues = conflicts(data),
    favorites = prefs.favorites,
    c = prefs.collection;
  let filtered = live.filter(
    (task) =>
      (status === "all" || task.completed === (status === "completed")) &&
      (tagFilter === null ||
        (tagFilter === ""
          ? !task.tags.length
          : task.tags.includes(tagFilter))) &&
      (view !== "favorites" || favorites.includes(task.id)) &&
      (!search.trim() ||
        [
          task.title,
          task.details,
          task.dueDate,
          ...task.checklist.map((s) => s.title),
          ...task.tags.map(
            (id) => data.tags.find((t) => t.id === id)?.name || "",
          ),
        ].some((s) =>
          s.toLocaleLowerCase().includes(search.trim().toLocaleLowerCase()),
        )),
  );
  if (view === "collection")
    filtered = filtered.filter(
      (t) =>
        (c.scope === "all" ||
          (c.scope === "today" && t.dueDate === today()) ||
          (c.scope === "upcoming" && t.dueDate > today()) ||
          (c.scope === "overdue" && !!t.dueDate && t.dueDate < today()) ||
          (c.scope === "noDate" && !t.dueDate)) &&
        (c.match === "untagged"
          ? !t.tags.length
          : !c.tags.length ||
            (c.match === "all"
              ? c.tags.every((id) => t.tags.includes(id))
              : c.tags.some((id) => t.tags.includes(id)))),
    );
  filtered = sortTasks(filtered, view === "collection" ? c.sort : sort);
  const group = (item: Task) =>
    view === "collection" && c.group === "calendar"
      ? item.dueDate || "noDate"
      : groupKey(item);
  const ungrouped =
    !!search ||
    tagFilter !== null ||
    view === "favorites" ||
    (view === "collection" && c.group === "none");
  const groups: [string, Task[]][] = ungrouped
    ? [["", filtered]]
    : view === "collection" && c.group === "calendar"
      ? [...new Set(filtered.map(group))]
          .sort((a, b) =>
            (a === "noDate" ? "9999" : a).localeCompare(
              b === "noDate" ? "9999" : b,
            ),
          )
          .map((key) => [key, filtered.filter((t) => group(t) === key)])
      : [
          "overdue",
          "today",
          "tomorrow",
          "thisWeek",
          "nextWeek",
          "later",
          "earlier",
          "noDate",
        ]
          .map(
            (key) =>
              [key, filtered.filter((t) => group(t) === key)] as [
                string,
                Task[],
              ],
          )
          .filter(([key, items]) => items.length || key === "today");
  const title =
    tagFilter !== null
      ? data.tags.find((t) => t.id === tagFilter)?.name || t("untagged")
      : t(view);
  const navigate = (next: string, id: string | null = null) => {
    setView(next);
    setTagFilter(id);
    setSearch("");
    setSelected([]);
    setSelecting(false);
  };
  const nav = (key: string, symbol: string) => (
    <button
      className={view === key && tagFilter === null ? "nav active" : "nav"}
      onClick={() => navigate(key)}
      aria-current={view === key && tagFilter === null ? "page" : undefined}
    >
      <span aria-hidden="true">{symbol}</span>
      {t(key)}
      {key === "agenda" && (
        <span className="count">{live.filter((t) => !t.completed).length}</span>
      )}
    </button>
  );
  return (
    <div className={`app ${prefs.compact ? "compact" : ""}`}>
      <aside className="sidebar">
        <div className="brand">
          <img src="/icons/app.svg" alt="" />
          <span>VerveTwoDo</span>
        </div>
        <button
          className="primary new-task"
          onClick={() =>
            setTask({
              ...newTask(prefs.rolloverDefault),
              tags: tagFilter ? [tagFilter] : [],
            })
          }
        >
          ＋ {t("newTask")}
          <kbd>⌘ N</kbd>
        </button>
        <nav aria-label="VerveTwoDo">
          {nav("agenda", "▤")}
          {nav("favorites", "☆")}
          {nav("collection", "▦")}
          {nav("tags", "◇")}
          <div className="nav-section">
            <span>{t("tags")}</span>
            <button
              className="icon-button"
              aria-label={t("newTag")}
              onClick={() => setTag(null)}
            >
              ＋
            </button>
          </div>
          {data.tags.map((tag) => (
            <button
              key={tag.id}
              className={tagFilter === tag.id ? "nav active" : "nav"}
              onClick={() => navigate("agenda", tag.id)}
            >
              <Artwork tag={tag} />
              <span className="ellipsis">{tag.name}</span>
              <span className="count">
                {
                  live.filter((t) => !t.completed && t.tags.includes(tag.id))
                    .length
                }
              </span>
            </button>
          ))}
          <button
            className={tagFilter === "" ? "nav active" : "nav"}
            onClick={() => navigate("agenda", "")}
          >
            <span>◇</span>
            {t("untagged")}
          </button>
        </nav>
        <div className="sidebar-bottom">
          <button
            className={`sync-pill ${issues.length ? "attention" : ""}`}
            onClick={() => setPanel("sync")}
          >
            <span className={`status-dot ${syncStatus}`} />
            {issues.length ? t("syncStatusConflict") : t(syncStatus)}
          </button>
          <button className="nav" onClick={() => setPanel("settings")}>
            <span aria-hidden="true">⚙</span>
            {t("settings")}
          </button>
        </div>
      </aside>
      <main className="workspace">
        <header className="page-header">
          <div>
            <p className="eyebrow">{t("localOnly")}</p>
            <h1>{title}</h1>
          </div>
          <div className="header-actions">
            <button
              className="mobile-add primary"
              onClick={() => setTask(newTask(prefs.rolloverDefault))}
            >
              ＋ {t("newTask")}
            </button>
            {view === "tags" ? (
              <button className="primary" onClick={() => setTag(null)}>
                ＋ {t("newTag")}
              </button>
            ) : (
              <button onClick={() => setPanel("options")}>
                {t("viewOptions")}
              </button>
            )}
          </div>
        </header>
        {update && (
          <div className="banner" role="status">
            {t("updateReady")}
            <button
              disabled={!!task || tag !== undefined || bulk}
              onClick={async () => {
                const reg = await navigator.serviceWorker.getRegistration();
                navigator.serviceWorker.addEventListener(
                  "controllerchange",
                  () => location.reload(),
                  { once: true },
                );
                reg?.waiting?.postMessage("activate");
              }}
            >
              {t("reload")}
            </button>
          </div>
        )}
        {widget && widgetOffer && data.connected && (
          <div className="banner" role="status">
            {t(widget === "macos" ? "widgetOfferMac" : "widgetOfferWindows")}
            {widget === "macos" ? (
              <button onClick={() => setPanel("settings")}>
                {t("widgetSetUp")}
              </button>
            ) : (
              <a className="button" href={widgetDownloads[widget]}>
                {t("widgetDownload")}
              </a>
            )}
            <button
              onClick={() => {
                widgetOfferDismissed(true);
                setWidgetOffer(false);
              }}
            >
              {t("widgetDismiss")}
            </button>
          </div>
        )}
        {issues.length > 0 && (
          <button
            className="banner conflict-banner"
            onClick={() => setPanel("sync")}
          >
            {t("syncAttentionTitle")} · {issues.length} →
          </button>
        )}
        <div className="toolbar">
          <div className="search">
            <span aria-hidden="true">⌕</span>
            <input
              ref={searchRef}
              aria-label={t(view === "tags" ? "searchTags" : "search")}
              placeholder={t(view === "tags" ? "searchTags" : "search")}
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          {view !== "tags" && (
            <>
              <select
                aria-label={t("status")}
                value={status}
                onChange={(e) => {
                  setStatus(e.target.value);
                  setSelected([]);
                }}
              >
                {["openTasks", "completed", "all"].map((key, i) => (
                  <option key={key} value={["open", "completed", "all"][i]}>
                    {t(key)}
                  </option>
                ))}
              </select>
              <button
                aria-pressed={selecting}
                onClick={() => {
                  setSelecting(!selecting);
                  setSelected([]);
                }}
              >
                {t("select")}
              </button>
            </>
          )}
        </div>
        {selecting && (
          <div className="selection-bar">
            <strong>
              {selected.length}{" "}
              {t(selected.length === 1 ? "selectedOne" : "selected")}
            </strong>
            <button onClick={() => setSelected(filtered.map((t) => t.id))}>
              {t("selectAll")}
            </button>
            <button onClick={() => setSelected([])}>{t("selectNone")}</button>
            <button disabled={!selected.length} onClick={() => setBulk(true)}>
              {t("bulkEdit")}
            </button>
            <button
              disabled={!selected.length}
              onClick={() =>
                action((d) => commit(d, completion(d, selected, true)))
              }
            >
              {t("complete")}
            </button>
            <button
              disabled={!selected.length}
              className="danger"
              onClick={() => remove(selected)}
            >
              {t("delete")}
            </button>
          </div>
        )}
        {view === "tags" ? (
          <div className="tag-list">
            {data.tags
              .filter((tag) =>
                tag.name.toLowerCase().includes(search.toLowerCase()),
              )
              .map((tag) => (
                <div className="tag-card" key={tag.id}>
                  <button
                    className="tag-open"
                    onClick={() => navigate("agenda", tag.id)}
                  >
                    <Artwork tag={tag} />
                    <strong>{tag.name}</strong>
                    <span className="muted">
                      {
                        live.filter(
                          (t) => t.tags.includes(tag.id) && !t.completed,
                        ).length
                      }{" "}
                      {t("taskCount")}
                    </span>
                  </button>
                  <button onClick={() => setTag(tag)}>{t("edit")}</button>
                  <button
                    className="danger"
                    aria-label={`${t("deleteTag")} ${tag.name}`}
                    onClick={() =>
                      setConfirm({
                        message: "deleteTagBody",
                        action: () =>
                          action((d) =>
                            commit(
                              d,
                              d.tasks
                                .filter((t) => t.tags.includes(tag.id))
                                .map((t) => ({
                                  ...t,
                                  tags: t.tags.filter((id) => id !== tag.id),
                                })),
                              d.tags.filter((t) => t.id !== tag.id),
                            ),
                          ),
                      })
                    }
                  >
                    {t("delete")}
                  </button>
                </div>
              ))}
            {!data.tags.length && (
              <div className="empty">
                <span>◇</span>
                <p>{t("noTags")}</p>
              </div>
            )}
          </div>
        ) : (
          <div className="task-groups">
            {groups.map(([key, items]) => (
              <section
                key={key}
                className={`task-group ${key === "overdue" ? "overdue" : ""}`}
              >
                {key && (
                  <button
                    className="group-heading"
                    aria-expanded={!collapsed.includes(key)}
                    onClick={() =>
                      setCollapsed(
                        collapsed.includes(key)
                          ? collapsed.filter((k) => k !== key)
                          : [...collapsed, key],
                      )
                    }
                  >
                    <span>{collapsed.includes(key) ? "›" : "⌄"}</span>
                    <h2>
                      {/^\d{4}-/.test(key)
                        ? new Intl.DateTimeFormat(locale(prefs.language), {
                            dateStyle: "full",
                          }).format(new Date(`${key}T12:00:00`))
                        : t(key)}
                    </h2>
                    <span className="count">{items.length}</span>
                  </button>
                )}
                {!collapsed.includes(key) && (
                  <div className="group-band">
                    {!items.length ? (
                      <p className="empty-group">
                        {t(key === "today" ? "todayEmpty" : "noResults")}
                      </p>
                    ) : (
                      items.map((item) => (
                        <article
                          className={`task-row ${item.completed ? "completed" : ""} ${selected.includes(item.id) ? "selected" : ""}`}
                          key={item.id}
                        >
                          {selecting ? (
                            <input
                              type="checkbox"
                              aria-label={`${t("select")} ${item.title}`}
                              checked={selected.includes(item.id)}
                              onChange={(e) =>
                                setSelected(
                                  e.target.checked
                                    ? [...selected, item.id]
                                    : selected.filter((id) => id !== item.id),
                                )
                              }
                            />
                          ) : (
                            <button
                              className="completion"
                              style={{
                                background: item.tags.length
                                  ? `conic-gradient(${item.tags.map((id, i) => `${data.tags.find((t) => t.id === id)?.color || "#687B91"} ${(i * 100) / item.tags.length}% ${((i + 1) * 100) / item.tags.length}%`).join(",")})`
                                  : undefined,
                              }}
                              aria-label={`${t(item.completed ? "reopen" : "complete")}: ${item.title}`}
                              onClick={() => toggle(item.id, !item.completed)}
                            >
                              <span>{item.completed ? "✓" : ""}</span>
                            </button>
                          )}
                          <button
                            className="task-open"
                            onClick={() => setTask(item)}
                          >
                            <span className="task-title">{item.title}</span>
                            <span className="metadata">
                              {[
                                item.dueDate &&
                                  new Intl.DateTimeFormat(
                                    locale(prefs.language),
                                    { month: "short", day: "numeric" },
                                  ).format(
                                    new Date(`${item.dueDate}T12:00:00`),
                                  ),
                                item.dueTime &&
                                  `${item.dueTime} ${item.timeZone}`,
                                item.priority > 0
                                  ? t(`priority${item.priority}`)
                                  : "",
                                item.checklist.length
                                  ? `${item.checklist.filter((s) => s.completed).length}/${item.checklist.length} ${t("checklist").toLowerCase()}`
                                  : "",
                              ]
                                .filter(Boolean)
                                .join(" · ")}
                              {item.recurrence && <TaskIcon name="repeat" label={t("recurrence")}/>}
                              {item.rollover && <TaskIcon name="rollover" label={t("rollover")}/>}
                            </span>
                          </button>
                          {(view !== "collection" || c.icons) &&
                            item.tags.length > 0 && (
                              <button
                                className="tag-strip"
                                aria-label={item.tags
                                  .map(
                                    (id) =>
                                      data.tags.find((t) => t.id === id)?.name,
                                  )
                                  .join(", ")}
                                onClick={() => setTask(item)}
                              >
                                {item.tags.slice(0, 2).map((id) => {
                                  const tag = data.tags.find(
                                    (t) => t.id === id,
                                  );
                                  return tag && <Artwork key={id} tag={tag} />;
                                })}
                                {item.tags.length > 2 && (
                                  <span className="overflow-count">
                                    +{item.tags.length - 2}
                                  </span>
                                )}
                              </button>
                            )}
                          <button
                            className="favorite icon-button"
                            aria-label={t(
                              favorites.includes(item.id)
                                ? "unfavorite"
                                : "favorite",
                            )}
                            aria-pressed={favorites.includes(item.id)}
                            onClick={() =>
                              action((d) => {
                                d.preferences.favorites =
                                  d.preferences.favorites.includes(item.id)
                                    ? d.preferences.favorites.filter(
                                        (id) => id !== item.id,
                                      )
                                    : [...d.preferences.favorites, item.id];
                              })
                            }
                          >
                            {favorites.includes(item.id) ? "★" : "☆"}
                          </button>
                        </article>
                      ))
                    )}
                  </div>
                )}
              </section>
            ))}
            {!live.length && (
              <div className="empty">
                <span>✓</span>
                <p>{t("empty")}</p>
                <button
                  className="primary"
                  onClick={() => setTask(newTask(prefs.rolloverDefault))}
                >
                  ＋ {t("newTask")}
                </button>
              </div>
            )}
          </div>
        )}
        <div className="workspace-footer">
          <span>
            {filtered.length}{" "}
            {t(filtered.length === 1 ? "taskCountOne" : "taskCount")}
          </span>
          <span>{t("localWeb")}</span>
          {data.undo && (
            <button onClick={() => action(undo, "undone")}>{t("undo")}</button>
          )}
        </div>
      </main>
      {notice && (
        <div className="toast" role="status">
          <span>{t(notice)}</span>
          <button aria-label={t("dismiss")} onClick={() => setNotice("")}>
            ×
          </button>
        </div>
      )}
      {task && (
        <TaskModal
          key={task.id}
          task={task}
          data={data}
          t={t}
          onClose={() => setTask(undefined)}
          onSave={save}
          onDelete={(id) => remove([id])}
          onComplete={toggle}
        />
      )}
      {tag !== undefined && (
        <TagModal
          tag={tag || undefined}
          t={t}
          onClose={() => setTag(undefined)}
          onSave={async (tag) => {
            await run((d) =>
              commit(
                d,
                [],
                [
                  ...d.tags.filter((t) => t.id !== tag.id),
                  { ...tag, name: tag.name.trim() },
                ],
              ),
            );
          }}
        />
      )}
      {bulk && (
        <BulkModal
          data={data}
          t={t}
          onClose={() => setBulk(false)}
          onSave={async (apply) => {
            await run((d) => {
              const changed = d.tasks
                .filter((t) => selected.includes(t.id) && !t.deleted)
                .map(apply);
              const staged = structuredClone(d);
              commit(
                staged,
                changed.map((t) => ({
                  ...t,
                  completed: d.tasks.find((n) => n.id === t.id)!.completed,
                })),
                staged.tags,
                false,
              );
              const extra = changed.flatMap((t) =>
                completion(staged, [t.id], t.completed),
              );
              const ids = new Set(extra.map((t) => t.id));
              commit(d, [...changed.filter((t) => !ids.has(t.id)), ...extra]);
            });
          }}
        />
      )}
      {confirm && (
        <Modal
          title={t("apply")}
          onClose={() => setConfirm(undefined)}
          footer={
            <>
              <button onClick={() => setConfirm(undefined)}>
                {t("cancel")}
              </button>
              <button
                className="danger"
                onClick={() => {
                  confirm.action();
                  setConfirm(undefined);
                }}
              >
                {t("apply")}
              </button>
            </>
          }
        >
          <p>{t(confirm.message)}</p>
        </Modal>
      )}
      {panel === "options" && (
        <Modal
          title={t("viewOptions")}
          onClose={() => setPanel("")}
          footer={
            <button className="primary" onClick={() => setPanel("")}>
              {t("done")}
            </button>
          }
        >
          <Field label={t("due")}>
            <select
              value={view === "collection" ? c.sort : sort}
              onChange={(e) =>
                view === "collection"
                  ? action((d) => {
                      d.preferences.collection.sort = e.target.value;
                    })
                  : setSort(e.target.value)
              }
            >
              {[
                "due",
                "latest",
                "newest",
                "oldest",
                "priority",
                "alphabetical",
              ].map((key) => (
                <option value={key} key={key}>
                  {t(key)}
                </option>
              ))}
            </select>
          </Field>
          <label className="toggle">
            <input
              type="checkbox"
              checked={prefs.compact}
              onChange={(e) => {
                const checked = e.target.checked;
                action((d) => {
                  d.preferences.compact = checked;
                });
              }}
            />
            {t("compact")}
          </label>
          {view === "collection" && (
            <>
              <p className="hint">{t("webWidgetHint")}</p>
              {[
                [
                  "scope",
                  "dateScope",
                  ["all", "today", "upcoming", "overdue", "noDate"],
                ],
                ["group", "grouping", ["relative", "calendar", "none"]],
                ["match", "tagMatching", ["any", "all", "untagged"]],
              ].map(([key, label, options]) => (
                <Field key={String(key)} label={t(String(label))}>
                  <select
                    value={c[key as "scope" | "group" | "match"]}
                    onChange={(e) => {
                      const v = e.target.value;
                      action((d) => {
                        d.preferences.collection[
                          key as "scope" | "group" | "match"
                        ] = v;
                      });
                    }}
                  >
                    {(options as string[]).map((v) => (
                      <option key={v} value={v}>
                        {t(
                          key === "match"
                            ? v === "any"
                              ? "matchAny"
                              : v === "all"
                                ? "matchAll"
                                : v
                            : v,
                        )}
                      </option>
                    ))}
                  </select>
                </Field>
              ))}
              <TagChoices
                t={t}
                tags={data.tags}
                selected={c.tags}
                onChange={(tags) =>
                  action((d) => {
                    d.preferences.collection.tags = tags;
                  })
                }
              />
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={c.icons}
                  onChange={(e) => {
                    const checked = e.target.checked;
                    action((d) => {
                      d.preferences.collection.icons = checked;
                    });
                  }}
                />
                {t("showIcons")}
              </label>
            </>
          )}
        </Modal>
      )}
      {panel === "settings" && (
        <Settings
          data={data}
          t={t}
          action={action}
          notice={setNotice}
          onClose={() => setPanel("")}
          onSync={() => setPanel("sync")}
          confirm={setConfirm}
        />
      )}
      {panel === "sync" && (
        <SyncSettings
          data={data}
          t={t}
          status={syncStatus}
          action={action}
          notice={setNotice}
          onClose={() => setPanel("")}
        />
      )}
    </div>
  );
}
