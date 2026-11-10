import React, { useEffect, useRef, useState, useId } from "react";
import {
  Data,
  Task,
  Tag,
  newTask,
  uuid,
  validateTask,
  equal,
  Recurrence,
  today,
  plusDays,
  nextDate,
  validDate,
  dateUTC,
} from "./model";
import { IconLabel } from "./TaskIcon";
import { Text, locale } from "./i18n";
import { composeTime, normalTime, timePartEdit } from "./time";
import { iconIds } from "./shared-icons";
import { marked } from "marked";
import DOMPurify from "dompurify";
export const icons = iconIds;
export function Artwork({ tag }: { tag: Tag }) {
  return (
    <span
      className="artwork"
      style={{ borderColor: tag.color }}
      title={tag.name}
    >
      {icons.includes(tag.icon) && tag.icon !== "tag" ? (
        <img src={`/category-icons/${tag.icon}.png`} alt="" />
      ) : (
        <span aria-hidden="true">◇</span>
      )}
    </span>
  );
}
export function Modal({
  title,
  children,
  footer,
  onClose,
  wide = false,
}: {
  title: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  onClose: () => void;
  wide?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const d = ref.current!;
    d.showModal();
    const previous = document.activeElement;
    return () => {
      d.close();
      if (previous instanceof HTMLElement) previous.focus();
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className={wide ? "modal wide" : "modal"}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      aria-label={title}
    >
      <header>
        <h2>{title}</h2>
        <button
          type="button"
          className="icon-button"
          onClick={onClose}
          aria-label={
            document.documentElement.lang === "fr" ? "Fermer" : "Close"
          }
        >
          ×
        </button>
      </header>
      <div className="modal-content">{children}</div>
      {footer && <footer>{footer}</footer>}
    </dialog>
  );
}
export function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  const id = useId();
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {React.Children.map(children, (child) =>
        React.isValidElement(child) &&
        ["input", "select", "textarea"].includes(String(child.type))
          ? React.cloneElement(child as React.ReactElement<{ id: string }>, {
              id,
            })
          : child,
      )}
    </div>
  );
}
export function TagChoices({
  tags,
  selected,
  onChange,
  t,
}: {
  tags: Tag[];
  selected: string[];
  onChange: (ids: string[]) => void;
  t: Text;
}) {
  return (
    <div className="chips">
      {tags.length ? (
        tags.map((tag) => (
          <button
            type="button"
            key={tag.id}
            aria-pressed={selected.includes(tag.id)}
            className={selected.includes(tag.id) ? "chip active" : "chip"}
            onClick={() =>
              onChange(
                selected.includes(tag.id)
                  ? selected.filter((id) => id !== tag.id)
                  : [...selected, tag.id],
              )
            }
          >
            <Artwork tag={tag} />
            {tag.name}
          </button>
        ))
      ) : (
        <p className="muted">{t("createTagsHint")}</p>
      )}
    </div>
  );
}
export function TimeInput({
  label,
  value,
  fallback,
  optional,
  t,
  onChange,
}: {
  label: string;
  value: string;
  fallback: string;
  optional: boolean;
  t: Text;
  onChange: (value: string) => void;
}) {
  const id = useId();
  const split = (v: string) => (v ? v.split(":") : ["", ""]);
  const [parts, setParts] = useState(() => split(value));
  const emitted = useRef(value);
  const focused = useRef(false);
  // Only adopt outside changes while not typing: a pending save would otherwise reset the draft mid-entry.
  useEffect(() => {
    if (!focused.current && value !== emitted.current) {
      emitted.current = value;
      setParts(split(value));
    }
  }, [value]);
  const defaults = normalTime(fallback).split(":");
  const edit = (index: number, raw: string, caret: number | null) => {
    const accepted = timePartEdit(parts[index], raw, caret, index ? 59 : 23);
    if (accepted === null) return;
    const next = index ? [parts[0], accepted] : [accepted, parts[1]];
    setParts(next);
    const result = composeTime(next[0], next[1], fallback);
    if (!result && !optional) return;
    emitted.current = result;
    if (result !== value) onChange(result);
  };
  return (
    <div className="field">
      <span className="field-label" id={id}>
        {label}
      </span>
      <div
        className="time-input"
        role="group"
        aria-labelledby={id}
        onFocus={() => (focused.current = true)}
        onBlur={(e) => {
          if (e.currentTarget.contains(e.relatedTarget as Node | null)) return;
          focused.current = false;
          setParts(split(emitted.current));
        }}
      >
        {[0, 1].map((index) => (
          <React.Fragment key={index}>
            {index === 1 && <span aria-hidden="true">:</span>}
            <input
              inputMode="numeric"
              autoComplete="off"
              aria-label={`${label}, ${t(index ? "minute" : "hour")}`}
              placeholder={defaults[index]}
              value={parts[index]}
              onFocus={(e) => e.currentTarget.select()}
              onChange={(e) =>
                edit(index, e.target.value, e.target.selectionStart)
              }
            />
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}
function scheduleDate(date: string, language: string) {
  return new Intl.DateTimeFormat(locale(language), {
    dateStyle: "medium",
    timeZone: "UTC",
  }).format(dateUTC(date));
}
export function DateFields({
  task,
  defaultTime,
  language,
  onChange,
  t,
}: {
  task: Task;
  defaultTime: string;
  language: string;
  onChange: (patch: Partial<Task>) => void;
  t: Text;
}) {
  const id = useId();
  const rule = task.recurrence;
  const day = today();
  const setDate = (dueDate: string) => {
    if (dueDate === task.dueDate) return;
    onChange(
      dueDate
        ? { dueDate, recurrence: rule && { ...rule, anchor: dueDate } }
        : { dueDate: "", dueTime: "", recurrence: null },
    );
  };
  let following = "";
  if (
    rule &&
    validDate(task.dueDate) &&
    Number.isInteger(rule.interval) &&
    rule.interval >= 1 &&
    rule.interval <= 999
  ) {
    try {
      following = scheduleDate(nextDate(task.dueDate, rule), language);
    } catch {
      /* A recurrence may exceed the supported calendar. */
    }
  }
  return (
    <div className="schedule-editor">
      <div className="schedule-heading">
        <label htmlFor={id}>
          <IconLabel name="agenda">{t("dueDate")}</IconLabel>
        </label>
        {task.dueDate && (
          <button
            type="button"
            className="clear-date"
            onClick={() => setDate("")}
          >
            {t("clearDate")}
          </button>
        )}
      </div>
      <input
        id={id}
        type="date"
        min="0001-01-01"
        max="9999-12-31"
        value={task.dueDate}
        onChange={(e) => setDate(e.target.value)}
      />
      <div className="schedule-shortcuts">
        {[
          { key: "today", days: 0 },
          { key: "tomorrow", days: 1 },
          { key: "nextWeek", days: 7 },
        ].map(({ key, days }) => {
          const date = plusDays(day, days);
          return (
            <button
              key={key}
              type="button"
              aria-pressed={task.dueDate === date}
              onClick={() => setDate(date)}
            >
              {t(key)}
            </button>
          );
        })}
      </div>
      {task.dueDate && (
        <>
          <label className="schedule-toggle">
            <IconLabel name="clock">{t("scheduleTime")}</IconLabel>
            <input
              type="checkbox"
              role="switch"
              checked={!!task.dueTime}
              onChange={(e) =>
                onChange({
                  dueTime: e.target.checked ? normalTime(defaultTime) : "",
                })
              }
            />
          </label>
          {task.dueTime && (
            <div className="schedule-settings">
              <TimeInput
                label={t("clockTime")}
                value={task.dueTime}
                fallback={defaultTime}
                optional={false}
                t={t}
                onChange={(dueTime) => onChange({ dueTime })}
              />
              <details className="schedule-zone">
                <summary>
                  {t("timeZone")} · {task.timeZone}
                </summary>
                <Field label={t("timeZone")}>
                  <input
                    list={id + "-zones"}
                    value={task.timeZone}
                    onChange={(e) => onChange({ timeZone: e.target.value })}
                  />
                  <datalist id={id + "-zones"}>
                    {(
                      (
                        Intl as unknown as {
                          supportedValuesOf?: (key: string) => string[];
                        }
                      ).supportedValuesOf?.("timeZone") || ["UTC"]
                    ).map((zone) => (
                      <option key={zone} value={zone} />
                    ))}
                  </datalist>
                </Field>
              </details>
            </div>
          )}
          <label className="schedule-toggle">
            <IconLabel name="repeat" off={!rule}>
              {t("recurrence")}
            </IconLabel>
            <input
              type="checkbox"
              role="switch"
              checked={!!rule}
              onChange={(e) =>
                onChange({
                  recurrence: e.target.checked
                    ? { frequency: "weekly", interval: 1, anchor: task.dueDate }
                    : null,
                })
              }
            />
          </label>
          {rule && (
            <div className="schedule-settings">
              <label className="schedule-caption" htmlFor={id + "-interval"}>
                {t("scheduleInterval")}
              </label>
              <div className="schedule-cadence">
                <input
                  id={id + "-interval"}
                  type="number"
                  min="1"
                  max="999"
                  required
                  value={rule.interval || ""}
                  onChange={(e) =>
                    onChange({
                      recurrence: { ...rule, interval: Number(e.target.value) },
                    })
                  }
                />
                <select
                  aria-label={t("repeatUnit")}
                  value={rule.frequency}
                  onChange={(e) =>
                    onChange({
                      recurrence: {
                        ...rule,
                        frequency: e.target.value as Recurrence["frequency"],
                        anchor: task.dueDate,
                      },
                    })
                  }
                >
                  {["daily", "weekly", "monthly", "yearly"].map((f) => (
                    <option key={f} value={f}>
                      {t(f + (rule.interval === 1 ? "Single" : "Plural"))}
                    </option>
                  ))}
                </select>
              </div>
              <p className="schedule-preview" aria-live="polite">
                {following
                  ? t("nextOccurrence").replace("%s", following)
                  : t(
                      rule.interval >= 1 && rule.interval <= 999
                        ? "invalidRecurrence"
                        : "repeatIntervalError",
                    )}
              </p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
const priorityPaths = [
  "M6 12h12",
  "m6 9 6 6 6-6",
  "M6 9h12M6 15h12",
  "m6 15 6-6 6 6",
  "m6 17 6-6 6 6m-12-6 6-6 6 6",
];

function PriorityScale({
  value,
  onChange,
  t,
  allowUnchanged = false,
}: {
  value: number | null;
  onChange: (value: number | null) => void;
  t: Text;
  allowUnchanged?: boolean;
}) {
  const name = useId();
  return (
    <fieldset className="priority-scale">
      <legend>{t("priority")}</legend>
      <div className="priority-value" aria-hidden="true">
        {t(value === null ? "unchanged" : `priority${value}`)}
      </div>
      <div className="priority-track">
        {priorityPaths.map((path, level) => (
          <label key={level} title={t(`priority${level}`)}>
            <input
              type="radio"
              name={name}
              value={level}
              aria-label={t(`priority${level}`)}
              checked={value === level}
              onChange={() => onChange(level)}
            />
            <span>
              <svg
                aria-hidden="true"
                viewBox="0 0 24 24"
                width="22"
                height="22"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d={path} />
              </svg>
            </span>
          </label>
        ))}
      </div>
      {allowUnchanged && (
        <label className="priority-unchanged">
          <input
            type="radio"
            name={name}
            checked={value === null}
            onChange={() => onChange(null)}
          />
          {t("unchanged")}
        </label>
      )}
    </fieldset>
  );
}

function ChecklistIcon({ action }: { action: "up" | "down" | "remove" }) {
  const path = {
    up: "m5 14 7-7 7 7",
    down: "m5 10 7 7 7-7",
    remove: "m6 6 12 12M18 6 6 18",
  }[action];
  return (
    <svg
      aria-hidden="true"
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d={path} />
    </svg>
  );
}
export function TaskModal({
  task,
  data,
  t,
  onClose,
  onSave,
  onDelete,
  onComplete,
}: {
  task: Task;
  data: Data;
  t: Text;
  onClose: () => void;
  onSave: (task: Task, base: Task) => Promise<Task>;
  onDelete: (id: string) => void;
  onComplete: (id: string, completed: boolean) => void;
}) {
  type FieldName = "title" | "notes" | "checklist" | "schedule" | "tags";
  const [active, setActive] = useState<FieldName | "new" | null>(
    task.createdAt ? null : "new",
  );
  const [base, setBase] = useState(task),
    [draft, setDraft] = useState(task);
  const [error, setError] = useState(""),
    [saving, setSaving] = useState(false);
  const [discard, setDiscard] = useState(false);
  const form = useRef<HTMLFormElement>(null);
  const stepFocus = useRef<string | null>(null);
  const trigger = useRef<FieldName | null>(null);
  const current = data.tasks.find((n) => n.id === task.id) || base;
  const creating = active === "new";
  const [stepText, setStepText] = useState("");
  const [stepChecked, setStepChecked] = useState(false);
  const composer = useRef<HTMLInputElement>(null);
  const checklistHeading = useRef<HTMLHeadingElement>(null);
  const resumeEntry = useRef(false);
  const draftChanged = active !== null && !equal(draft, base);
  const dirty = draftChanged || !!stepText.trim();
  const checklist =
    creating || active === "checklist" ? draft.checklist : current.checklist;
  function clearEntry() {
    setStepText("");
    setStepChecked(false);
  }
  function appendEntry(items: Task["checklist"]) {
    return [
      ...items,
      { id: uuid(), title: stepText.trim(), completed: stepChecked },
    ];
  }
  useEffect(() => {
    if (!dirty) return;
    const warn = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);
  useEffect(() => {
    if (resumeEntry.current) return;
    if (active && active !== "new") {
      const selectedStep =
        active === "checklist" && stepFocus.current
          ? Array.from(
              form.current?.querySelectorAll<HTMLInputElement>("[data-step]") ||
                [],
            ).find((input) => input.dataset.step === stepFocus.current)
          : undefined;
      (
        selectedStep ||
        form.current?.querySelector<HTMLElement>(
          "[data-editor] input:not([type=checkbox]), [data-editor] textarea, [data-editor] button",
        )
      )?.focus();
    } else if (!active && trigger.current) {
      form.current
        ?.querySelector<HTMLElement>(`[data-open="${trigger.current}"]`)
        ?.focus();
    }
  }, [active]);
  useEffect(() => {
    if (!saving && resumeEntry.current) {
      resumeEntry.current = false;
      if (composer.current?.disabled) checklistHeading.current?.focus();
      else composer.current?.focus();
    }
  }, [saving]);
  const patch = (p: Partial<Task>) => setDraft((d) => ({ ...d, ...p }));
  const close = () => {
    if (saving) return;
    if (dirty) setDiscard(true);
    else onClose();
  };
  async function change(
    next: FieldName | null,
    immediate?: Partial<Task>,
    continueEntry = false,
  ) {
    if (saving || (active && !form.current?.reportValidity())) return;
    const original = active ? base : current;
    let value = draftChanged || creating ? draft : current;
    if (immediate) value = { ...value, ...immediate };
    if (stepText.trim())
      value = { ...value, checklist: appendEntry(value.checklist) };
    try {
      setError("");
      setSaving(true);
      resumeEntry.current = continueEntry;
      if (dirty || creating || immediate) {
        validateTask(value);
        value = await onSave(value, original);
      }
      setBase(value);
      setDraft(value);
      clearEntry();
      if (next) trigger.current = next;
      setActive(next);
    } catch (e) {
      setError(e instanceof Error ? e.message : "error");
    } finally {
      setSaving(false);
    }
  }
  function cancel() {
    setError("");
    setDraft(base);
    setActive(null);
  }
  const step = (id: string, p: Partial<Task["checklist"][number]>) =>
    patch({
      checklist: draft.checklist.map((s) => (s.id === id ? { ...s, ...p } : s)),
    });
  const move = (index: number, by: number) => {
    const steps = [...draft.checklist];
    [steps[index], steps[index + by]] = [steps[index + by], steps[index]];
    patch({ checklist: steps });
  };
  const showing = (field: FieldName) => creating || active === field;
  function opener(
    field: FieldName,
    content: React.ReactNode,
    label: string,
    stepId?: string,
  ) {
    return (
      <button
        type="button"
        className="inline-value"
        data-open={field}
        aria-label={label}
        disabled={saving}
        onClick={() => {
          stepFocus.current = stepId || null;
          void change(field);
        }}
      >
        <span className="inline-content">{content}</span>
        {field !== "checklist" && label !== t("addNotes") && (
          <svg
            className="edit-mark"
            aria-hidden="true"
            viewBox="0 0 24 24"
            width="18"
            height="18"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="m15 5 4 4M4 20l4-1 12-12a2.8 2.8 0 0 0-4-4L4 15z" />
          </svg>
        )}
      </button>
    );
  }
  function editor(field: FieldName, children: React.ReactNode) {
    return (
      <div data-editor={active === field ? "" : undefined}>
        {children}
        {active === field && (
          <div className="field-actions">
            <button type="button" onClick={cancel}>
              {t("cancel")}
            </button>
            <button className="primary" type="submit">
              {t("save")}
            </button>
          </div>
        )}
      </div>
    );
  }
  const editLabel = (key: string) => `${t("edit")} · ${t(key)}`;
  const scheduleSummary = [
    current.dueDate
      ? scheduleDate(current.dueDate, data.preferences.language)
      : t("noDate"),
    current.dueTime && `${current.dueTime} (${current.timeZone})`,
    current.recurrence &&
      t(
        current.recurrence.frequency +
          (current.recurrence.interval === 1 ? "Once" : "Multiple"),
      ).replace("%d", String(current.recurrence.interval)),
  ]
    .filter(Boolean)
    .join(" · ");
  return (
    <>
      <Modal
        title={t(creating ? "newTask" : "taskDetails")}
        wide
        onClose={close}
        footer={
          creating ? (
            <>
              <button disabled={saving} onClick={close}>
                {t("cancel")}
              </button>
              <button
                className="primary"
                disabled={saving}
                onClick={() => form.current?.requestSubmit()}
              >
                {t("save")}
              </button>
            </>
          ) : (
            <>
              <button
                className="danger"
                disabled={saving || dirty}
                onClick={() => onDelete(current.id)}
              >
                {t("delete")}
              </button>
              <span className="spacer" />
              <button disabled={saving} onClick={close}>
                {t("close")}
              </button>
            </>
          )
        }
      >
        {error && (
          <p role="alert" className="error">
            {t(error)}
          </p>
        )}
        <form
          ref={form}
          className="task-layout"
          onSubmit={(e) => {
            e.preventDefault();
            if (active) void change(null);
          }}
          onKeyDown={(e) => {
            if (active && (e.ctrlKey || e.metaKey) && e.key === "Enter") {
              e.preventDefault();
              form.current?.requestSubmit();
            }
            if (active && !creating && e.key === "Escape") {
              e.preventDefault();
              if (!saving) cancel();
            }
          }}
        >
          <fieldset disabled={saving} className="task-body task-column">
            {showing("title") ? (
              editor(
                "title",
                <>
                  <Field label={t("title")}>
                    <input
                      autoFocus={creating}
                      className="title-input"
                      required
                      maxLength={16000}
                      value={draft.title}
                      onChange={(e) => patch({ title: e.target.value })}
                    />
                  </Field>
                </>,
              )
            ) : (
              <h1 className="detail-title" aria-label={current.title}>
                {opener("title", current.title, editLabel("title"))}
              </h1>
            )}
            {!creating && (
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={current.completed}
                  disabled={dirty}
                  onChange={(e) => {
                    cancel();
                    onComplete(current.id, e.target.checked);
                  }}
                />
                {t(current.completed ? "completed" : "openTasks")}
              </label>
            )}
            {showing("notes") ? (
              editor(
                "notes",
                <>
                  <Field label={t("notes")}>
                    <textarea
                      rows={9}
                      maxLength={100000}
                      value={draft.details}
                      onChange={(e) => patch({ details: e.target.value })}
                    />
                  </Field>
                  <p className="hint">{t("markdownHint")}</p>
                </>,
              )
            ) : (
              <>
                <h3>
                  {current.details
                    ? opener("notes", t("notes"), editLabel("notes"))
                    : t("notes")}
                </h3>
                {current.details ? (
                  <div
                    className="markdown inline-markdown"
                    onClick={(e) => {
                      if (
                        !(e.target as Element).closest("a") &&
                        !window.getSelection()?.toString()
                      )
                        void change("notes");
                    }}
                    dangerouslySetInnerHTML={{
                      __html: DOMPurify.sanitize(
                        marked.parse(current.details, {
                          async: false,
                        }) as string,
                      ),
                    }}
                  />
                ) : (
                  opener("notes", t("addNotes"), t("addNotes"))
                )}
              </>
            )}
            <h3 ref={checklistHeading} tabIndex={-1}>
              <IconLabel name="checklist">{t("checklist")}</IconLabel>
            </h3>
            {showing("checklist") ? (
              editor(
                "checklist",
                <>
                  <div className="steps">
                    {draft.checklist.map((s, i) => (
                      <div className="step" key={s.id}>
                        <div className="step-field">
                          <label className="step-check">
                            <input
                              type="checkbox"
                              aria-label={t(
                                s.completed ? "stepDone" : "stepTodo",
                              )}
                              checked={s.completed}
                              onChange={(e) =>
                                step(s.id, { completed: e.target.checked })
                              }
                            />
                          </label>
                          <input
                            data-step={s.id}
                            aria-label={`${t("step")} ${i + 1}`}
                            required
                            maxLength={16000}
                            value={s.title}
                            onChange={(e) =>
                              step(s.id, { title: e.target.value })
                            }
                          />
                        </div>
                        <div className="step-actions">
                          <button
                            type="button"
                            disabled={!i}
                            aria-label={t("moveUp")}
                            onClick={() => move(i, -1)}
                          >
                            <ChecklistIcon action="up" />
                          </button>
                          <button
                            type="button"
                            disabled={i === draft.checklist.length - 1}
                            aria-label={t("moveDown")}
                            onClick={() => move(i, 1)}
                          >
                            <ChecklistIcon action="down" />
                          </button>
                          <button
                            type="button"
                            aria-label={t("remove")}
                            onClick={() =>
                              patch({
                                checklist: draft.checklist.filter(
                                  (n) => n.id !== s.id,
                                ),
                              })
                            }
                          >
                            <ChecklistIcon action="remove" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </>,
              )
            ) : (
              <>
                {current.checklist.map((s) => (
                  <div className="inline-step" key={s.id}>
                    <label className="step-check">
                      <input
                        type="checkbox"
                        aria-label={s.title}
                        checked={s.completed}
                        onChange={() =>
                          void change(null, {
                            checklist: current.checklist.map((item) =>
                              item.id === s.id
                                ? { ...item, completed: !item.completed }
                                : item,
                            ),
                          })
                        }
                      />
                    </label>
                    {opener(
                      "checklist",
                      <span className={s.completed ? "completed" : ""}>
                        {s.title}
                      </span>,
                      s.title,
                      s.id,
                    )}
                  </div>
                ))}
              </>
            )}
            <div className="step-field step-composer">
              <label className="step-check">
                <input
                  type="checkbox"
                  aria-label={t("newStepDone")}
                  checked={stepChecked}
                  disabled={checklist.length >= 500}
                  onChange={(e) => {
                    setStepChecked(e.target.checked);
                    composer.current?.focus();
                  }}
                />
              </label>
              <input
                ref={composer}
                aria-label={t("newStep")}
                placeholder={t("newStep")}
                maxLength={16000}
                disabled={checklist.length >= 500}
                value={stepText}
                enterKeyHint="enter"
                onChange={(e) => setStepText(e.target.value)}
                onKeyDown={(e) => {
                  if (
                    e.nativeEvent.isComposing ||
                    e.nativeEvent.keyCode === 229
                  )
                    return;
                  if (e.key !== "Enter" && e.key !== "Escape") return;
                  e.preventDefault();
                  e.stopPropagation();
                  if (e.key === "Escape") {
                    clearEntry();
                    checklistHeading.current?.focus();
                  } else if (stepText.trim() && !e.repeat) {
                    if (creating) {
                      patch({ checklist: appendEntry(checklist) });
                      clearEntry();
                      if (checklist.length === 499)
                        checklistHeading.current?.focus();
                    } else void change(null, undefined, true);
                  }
                }}
              />
            </div>
          </fieldset>
          <fieldset disabled={saving} className="task-meta task-column">
            <PriorityScale
              value={creating ? draft.priority : current.priority}
              t={t}
              onChange={(priority) => {
                if (priority !== null) {
                  if (creating) patch({ priority });
                  else void change(null, { priority });
                }
              }}
            />

            {showing("schedule") ? (
              editor(
                "schedule",
                <>
                  <DateFields
                    task={draft}
                    language={data.preferences.language}
                    defaultTime={data.preferences.reminderTime}
                    t={t}
                    onChange={patch}
                  />
                </>,
              )
            ) : (
              <>
                <h3>
                  <IconLabel name="agenda">{t("dueDate")}</IconLabel>
                </h3>
                {opener("schedule", scheduleSummary, editLabel("dueDate"))}
              </>
            )}
            <label className="schedule-toggle rollover-setting">
              <IconLabel
                name="rollover"
                off={!(creating ? draft.rollover : current.rollover)}
              >
                {t("rollover")}
              </IconLabel>
              <input
                type="checkbox"
                checked={creating ? !!draft.rollover : !!current.rollover}
                onChange={(e) =>
                  creating
                    ? patch({ rollover: e.target.checked })
                    : void change(null, { rollover: e.target.checked })
                }
              />
            </label>
            <h3>
              <IconLabel name="tags">{t("tags")}</IconLabel>
            </h3>
            {showing("tags")
              ? editor(
                  "tags",
                  <>
                    <TagChoices
                      tags={data.tags}
                      selected={draft.tags}
                      onChange={(tags) => patch({ tags })}
                      t={t}
                    />
                  </>,
                )
              : opener(
                  "tags",
                  current.tags.length ? (
                    <span className="chips">
                      {data.tags
                        .filter((tag) => current.tags.includes(tag.id))
                        .map((tag) => (
                          <span className="chip" key={tag.id}>
                            <Artwork tag={tag} />
                            {tag.name}
                          </span>
                        ))}
                    </span>
                  ) : (
                    t("untagged")
                  ),
                  editLabel("tags"),
                )}
          </fieldset>
        </form>
      </Modal>
      {discard && (
        <Modal
          title={t("discardTitle")}
          onClose={() => setDiscard(false)}
          footer={
            <>
              <button onClick={() => setDiscard(false)}>
                {t("keepEditing")}
              </button>
              <button className="danger" onClick={onClose}>
                {t("discard")}
              </button>
            </>
          }
        >
          <p>{t("discardBody")}</p>
        </Modal>
      )}
    </>
  );
}
export function TagModal({
  tag,
  t,
  onClose,
  onSave,
}: {
  tag?: Tag;
  t: Text;
  onClose: () => void;
  onSave: (tag: Tag) => Promise<void>;
}) {
  const [draft, setDraft] = useState(
      tag || { id: uuid(), name: "", color: "#3877B8", icon: "tag" },
    ),
    [error, setError] = useState(""),
    [query, setQuery] = useState("");
  const form = useRef<HTMLFormElement>(null);
  return (
    <Modal
      title={t(tag ? "editTag" : "newTag")}
      onClose={onClose}
      footer={
        <>
          <button onClick={onClose}>{t("cancel")}</button>
          <button
            className="primary"
            onClick={() => form.current?.requestSubmit()}
          >
            {t("save")}
          </button>
        </>
      }
    >
      <form
        ref={form}
        onSubmit={async (e) => {
          e.preventDefault();
          try {
            await onSave(draft);
            onClose();
          } catch (e) {
            setError(e instanceof Error ? e.message : "invalidTag");
          }
        }}
      >
        {error && (
          <p className="error" role="alert">
            {t(error)}
          </p>
        )}
        <Field label={t("tagName")}>
          <input
            autoFocus
            required
            maxLength={100}
            value={draft.name}
            onChange={(e) => setDraft({ ...draft, name: e.target.value })}
          />
        </Field>
        <Field label={t("tagColor")}>
          <div className="color-field">
            <input
              type="color"
              aria-label={t("tagColorWheel")}
              value={draft.color}
              onChange={(e) => setDraft({ ...draft, color: e.target.value })}
            />
            <input
              pattern="#[0-9a-fA-F]{6}"
              required
              aria-label={t("tagColor")}
              value={draft.color}
              onChange={(e) => setDraft({ ...draft, color: e.target.value })}
            />
          </div>
        </Field>
        <Field label={t("iconSearch")}>
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </Field>
        <div className="icon-grid">
          {icons
            .filter((id) =>
              t(`categoryIcon.${id}`)
                .toLowerCase()
                .includes(query.toLowerCase()),
            )
            .map((icon) => (
              <button
                type="button"
                key={icon}
                title={t(`categoryIcon.${icon}`)}
                aria-label={t(`categoryIcon.${icon}`)}
                aria-pressed={draft.icon === icon}
                onClick={() => setDraft({ ...draft, icon })}
              >
                <Artwork tag={{ ...draft, icon }} />
              </button>
            ))}
        </div>
      </form>
    </Modal>
  );
}
export function BulkModal({
  data,
  t,
  onClose,
  onSave,
}: {
  data: Data;
  t: Text;
  onClose: () => void;
  onSave: (apply: (task: Task) => Task) => Promise<void>;
}) {
  const [priority, setPriority] = useState(""),
    [status, setStatus] = useState(""),
    [rollover, setRollover] = useState(""),
    [dateChange, setDateChange] = useState(false),
    [draft, setDraft] = useState(newTask()),
    [mode, setMode] = useState("add"),
    [tags, setTags] = useState<string[]>([]),
    [error, setError] = useState("");
  return (
    <Modal
      title={t("bulkEdit")}
      onClose={onClose}
      footer={
        <>
          <button onClick={onClose}>{t("cancel")}</button>
          <button
            className="primary"
            onClick={async () => {
              try {
                await onSave((t) => ({
                  ...t,
                  ...(priority !== "" ? { priority: Number(priority) } : {}),
                  ...(status !== "" ? { completed: status === "true" } : {}),
                  ...(rollover !== "" ? { rollover: rollover === "true" } : {}),
                  ...(dateChange
                    ? {
                        dueDate: draft.dueDate,
                        dueTime: draft.dueTime,
                        timeZone: draft.timeZone,
                        recurrence: draft.recurrence,
                      }
                    : {}),
                  tags:
                    mode === "replace"
                      ? tags
                      : mode === "remove"
                        ? t.tags.filter((id) => !tags.includes(id))
                        : [...new Set([...t.tags, ...tags])],
                }));
                onClose();
              } catch (e) {
                setError(e instanceof Error ? e.message : "error");
              }
            }}
          >
            {t("apply")}
          </button>
        </>
      }
    >
      {error && (
        <p role="alert" className="error">
          {t(error)}
        </p>
      )}
      <PriorityScale
        value={priority === "" ? null : Number(priority)}
        allowUnchanged
        t={t}
        onChange={(value) => setPriority(value === null ? "" : String(value))}
      />
      {[
        [t("status"), status, setStatus, t("completed"), t("openTasks")],
        [
          t("rollover"),
          rollover,
          setRollover,
          t("rolloverOn"),
          t("rolloverOff"),
        ],
      ].map(([label, v, set, yes, no]) => (
        <Field key={String(label)} label={String(label)}>
          <select
            value={String(v)}
            onChange={(e) => (set as (s: string) => void)(e.target.value)}
          >
            <option value="">{t("unchanged")}</option>
            <option value="true">{String(yes)}</option>
            <option value="false">{String(no)}</option>
          </select>
        </Field>
      ))}
      <label className="toggle">
        <input
          type="checkbox"
          checked={dateChange}
          onChange={(e) => setDateChange(e.target.checked)}
        />
        {t("dueDate")}
      </label>
      {dateChange && (
        <>
          <p className="hint">{t("emptyClearsDate")}</p>
          <DateFields
            task={draft}
            language={data.preferences.language}
            defaultTime={data.preferences.reminderTime}
            t={t}
            onChange={(p) => setDraft({ ...draft, ...p })}
          />
        </>
      )}
      <Field label={t("tags")}>
        <select value={mode} onChange={(e) => setMode(e.target.value)}>
          {["add", "remove", "replace"].map((m) => (
            <option key={m} value={m}>
              {t(`${m}Tags`)}
            </option>
          ))}
        </select>
      </Field>
      <TagChoices tags={data.tags} selected={tags} onChange={setTags} t={t} />
    </Modal>
  );
}
