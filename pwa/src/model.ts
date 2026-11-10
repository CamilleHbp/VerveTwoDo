import { md5 } from "@noble/hashes/legacy.js";
export type Step = { id: string; title: string; completed: boolean };
export type Recurrence = {
  frequency: "daily" | "weekly" | "monthly" | "yearly";
  interval: number;
  anchor: string;
};
export type Task = {
  id: string;
  title: string;
  details: string;
  completed: boolean;
  tags: string[];
  priority: number;
  dueDate: string;
  dueTime: string;
  timeZone: string;
  checklist: Step[];
  recurrence: Recurrence | null;
  createdAt: number;
  modifiedAt: number;
  generation: number;
  deleted: boolean;
  retracted: boolean;
  parentId: string;
  rollover: boolean | null;
};
export type Tag = { id: string; name: string; color: string; icon: string };
export type Version = {
  id: string;
  value: string;
  source?: { id: string; name: string; platform: string };
};
export type RecordValue = {
  id: string;
  kind: string;
  fields: Record<string, Version[]>;
};
export type Operation = {
  id: string;
  record: string;
  kind: string;
  field: string;
  value: string;
  replaces: string[];
  source?: Version["source"];
};
export type Preferences = {
  language: string;
  theme: string;
  rolloverDefault: boolean;
  reminders: boolean;
  push?: boolean;
  reminderTime: string;
  advance: number;
  favorites: string[];
  compact: boolean;
  collection: {
    scope: string;
    tags: string[];
    match: string;
    sort: string;
    group: string;
    icons: boolean;
  };
};
export type Data = {
  version: 1;
  revision: number;
  tasks: Task[];
  tags: Tag[];
  records: RecordValue[];
  outbox: Operation[];
  connected: boolean;
  source: string;
  preferences: Preferences;
  notified: Record<string, number>;
  undo?: { before: Task[]; after: Task[]; tagsBefore: Tag[]; tagsAfter: Tag[] };
};
export const uuid = () => crypto.randomUUID();
export const equal = (a: unknown, b: unknown) =>
  JSON.stringify(a) === JSON.stringify(b);
export const initial = (): Data => ({
  version: 1,
  revision: 0,
  tasks: [],
  tags: [],
  records: [],
  outbox: [],
  connected: false,
  source: uuid(),
  notified: {},
  preferences: {
    language: "system",
    theme: "system",
    rolloverDefault: false,
    reminders: false,
    reminderTime: "09:00",
    advance: 0,
    favorites: [],
    compact: false,
    collection: {
      scope: "all",
      tags: [],
      match: "any",
      sort: "due",
      group: "relative",
      icons: true,
    },
  },
});
export function newTask(rollover = false): Task {
  return {
    id: uuid(),
    title: "",
    details: "",
    completed: false,
    tags: [],
    priority: 0,
    dueDate: "",
    dueTime: "",
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    checklist: [],
    recurrence: null,
    createdAt: 0,
    modifiedAt: 0,
    generation: 0,
    deleted: false,
    retracted: false,
    parentId: "",
    rollover,
  };
}
export function today(now = new Date()) {
  return `${String(now.getFullYear()).padStart(4, "0")}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}
export function dateUTC(value: string) {
  return new Date(`${value}T12:00:00Z`);
}
export function validDate(value: string) {
  return (
    /^\d{4}-\d{2}-\d{2}$/.test(value) &&
    value >= "0001-01-01" &&
    value <= "9999-12-31" &&
    !isNaN(+dateUTC(value)) &&
    dateUTC(value).toISOString().slice(0, 10) === value
  );
}
export function plusDays(date: string, n: number) {
  const d = dateUTC(date);
  d.setUTCDate(d.getUTCDate() + n);
  return d.toISOString().slice(0, 10);
}
export function validateTask(t: Task) {
  if (
    typeof t.id !== "string" ||
    t.id.length < 16 ||
    t.id.length > 128 ||
    typeof t.title !== "string" ||
    !t.title.trim() ||
    t.title.length > 16000
  )
    throw Error("invalidTitle");
  if (
    typeof t.details !== "string" ||
    t.details.length > 100000 ||
    !Number.isInteger(t.priority) ||
    t.priority < 0 ||
    t.priority > 4 ||
    !Array.isArray(t.tags) ||
    t.tags.length > 100 ||
    new Set(t.tags).size !== t.tags.length ||
    t.tags.some((x) => typeof x !== "string" || x.length < 16 || x.length > 128)
  )
    throw Error("invalidTask");
  if (
    !Array.isArray(t.checklist) ||
    t.checklist.length > 500 ||
    new Set(t.checklist.map((s) => s.id)).size !== t.checklist.length ||
    t.checklist.some(
      (s) =>
        !s.id ||
        typeof s.title !== "string" ||
        !s.title.trim() ||
        s.title.length > 16000 ||
        typeof s.completed !== "boolean",
    )
  )
    throw Error("invalidChecklist");
  if (typeof t.dueDate !== "string" || (t.dueDate && !validDate(t.dueDate)))
    throw Error("invalidDate");
  if (t.dueTime) {
    if (!t.dueDate || !/^([01]\d|2[0-3]):[0-5]\d$/.test(t.dueTime))
      throw Error("invalidTime");
    try {
      new Intl.DateTimeFormat("en", { timeZone: t.timeZone });
    } catch {
      throw Error("invalidTime");
    }
  }
  if (
    t.recurrence &&
    (!t.dueDate ||
      !["daily", "weekly", "monthly", "yearly"].includes(
        t.recurrence.frequency,
      ) ||
      !Number.isInteger(t.recurrence.interval) ||
      t.recurrence.interval < 1 ||
      t.recurrence.interval > 999 ||
      !validDate(t.recurrence.anchor))
  )
    throw Error("invalidRecurrence");
  for (const key of ["completed", "deleted", "retracted"] as const)
    if (typeof t[key] !== "boolean") throw Error("invalidTask");
}
export function validateTags(tags: Tag[]) {
  if (
    new Set(tags.map((t) => t.id)).size !== tags.length ||
    new Set(tags.map((t) => t.name.trim().toLowerCase())).size !==
      tags.length ||
    tags.some(
      (t) =>
        typeof t.id !== "string" ||
        t.id.length < 16 ||
        t.id.length > 128 ||
        !t.name.trim() ||
        t.name.length > 100 ||
        !/^#[\da-f]{6}$/i.test(t.color) ||
        !/^[\w-]{1,64}$/.test(t.icon),
    )
  )
    throw Error("invalidTag");
}
export function nextDate(date: string, r: Recurrence) {
  if (r.frequency === "daily" || r.frequency === "weekly") {
    const result = plusDays(
      date,
      r.interval * (r.frequency === "weekly" ? 7 : 1),
    );
    if (!validDate(result)) throw Error("invalidRecurrence");
    return result;
  }
  const d = dateUTC(date),
    a = dateUTC(r.anchor);
  let y = d.getUTCFullYear(),
    m = d.getUTCMonth();
  if (r.frequency === "monthly") {
    m += r.interval;
    y += Math.floor(m / 12);
    m %= 12;
  } else {
    y += r.interval;
    m = a.getUTCMonth();
  }
  if (y > 9999) throw Error("invalidRecurrence");
  const last = new Date(0);
  last.setUTCFullYear(y, m + 1, 0);
  const day = Math.min(a.getUTCDate(), last.getUTCDate());
  return `${String(y).padStart(4, "0")}-${String(m + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}
// Java UUID.nameUUIDFromBytes parity, including its no-namespace MD5 input.
export function successorID(id: string, date: string) {
  const h = md5(new TextEncoder().encode(`${id}:${date}`));
  h[6] = (h[6] & 15) | 48;
  h[8] = (h[8] & 63) | 128;
  const s = Array.from(h, (x) => x.toString(16).padStart(2, "0")).join("");
  return `${s.slice(0, 8)}-${s.slice(8, 12)}-${s.slice(12, 16)}-${s.slice(16, 20)}-${s.slice(20)}`;
}
export function completion(d: Data, ids: string[], completed: boolean) {
  const changed: Task[] = [];
  for (const t of d.tasks.filter(
    (t) => ids.includes(t.id) && !t.deleted && t.completed !== completed,
  )) {
    changed.push({ ...t, completed });
    if (completed && t.recurrence && t.dueDate) {
      const date = nextDate(t.dueDate, t.recurrence),
        id = successorID(t.id, date),
        old = d.tasks.find((t) => t.id === id);
      if (!old || old.retracted)
        changed.push({
          ...t,
          id,
          completed: false,
          dueDate: date,
          deleted: false,
          retracted: false,
          checklist: t.checklist.map((s) => ({ ...s, completed: false })),
          parentId: t.id,
        });
    } else if (!completed)
      changed.push(
        ...d.tasks
          .filter(
            (n) => n.parentId === t.id && n.generation === 1 && !n.deleted,
          )
          .map((n) => ({ ...n, deleted: true, retracted: true })),
      );
  }
  return changed;
}
const taskKeys = [
  "title",
  "details",
  "completed",
  "priority",
  "dueDate",
  "dueTime",
  "timeZone",
  "checklist",
  "recurrence",
  "tags",
  "parentId",
  "deleted",
  "retracted",
  "createdAt",
  "rollover",
] as const;
export function fields(t: Task): Record<string, string> {
  return Object.fromEntries(
    taskKeys
      .filter((k) => k !== "rollover" || t.rollover !== null)
      .map((k) => [k, typeof t[k] === "string" ? t[k] : JSON.stringify(t[k])]),
  );
}
export function value(r: RecordValue, k: string) {
  return (
    [...(r.fields[k] || [])].sort((a, b) => a.id.localeCompare(b.id))[0]
      ?.value ?? ""
  );
}
export function predict(
  records: RecordValue[],
  ops: Operation[],
): RecordValue[] {
  const map = new Map(structuredClone(records).map((r) => [r.id, r]));
  for (const o of ops) {
    const r = map.get(o.record) || { id: o.record, kind: o.kind, fields: {} };
    r.fields[o.field] = [
      ...(r.fields[o.field] || []).filter(
        (v) => !o.replaces.includes(v.id) && v.id !== o.id,
      ),
      { id: o.id, value: o.value, source: o.source },
    ];
    map.set(r.id, r);
  }
  return [...map.values()];
}
export function plan(d: Data, changed: Task[], tags: Tag[]) {
  const ops: Operation[] = [];
  const desired: [string, string, Record<string, string>][] = [
    ...tags.map(
      (t) =>
        [
          t.id,
          "tag",
          { name: t.name, color: t.color, icon: t.icon, deleted: "false" },
        ] as [string, string, Record<string, string>],
    ),
    ...changed.map(
      (t) =>
        [t.id, "task", fields(t)] as [string, string, Record<string, string>],
    ),
    ...d.tags
      .filter((t) => !tags.some((n) => n.id === t.id))
      .map(
        (t) =>
          [t.id, "tag", { deleted: "true" }] as [
            string,
            string,
            Record<string, string>,
          ],
      ),
  ];
  for (const [id, kind, fs] of desired) {
    const old = d.records.find((r) => r.id === id);
    for (const [field, v] of Object.entries(fs)) {
      if (old && value(old, field) === v) continue;
      ops.push({
        id: uuid(),
        record: id,
        kind,
        field,
        value: v,
        replaces: old?.fields[field]?.map((v) => v.id) || [],
        source: { id: d.source, name: "VerveTwoDo Web", platform: "linux" },
      });
    }
  }
  return ops;
}
export function commit(
  d: Data,
  changed: Task[],
  tags = d.tags,
  remember = true,
) {
  validateTags(tags);
  const now = Date.now();
  const updates = changed.map((t) => {
    validateTask(t);
    const old = d.tasks.find((o) => o.id === t.id);
    return {
      ...t,
      title: t.title.trim(),
      createdAt:
        old?.createdAt ||
        d.tasks.find((o) => o.id === t.parentId)?.createdAt ||
        now,
      modifiedAt: now,
      generation: (old?.generation || 0) + 1,
    };
  });
  const before = structuredClone(
    d.tasks.filter((t) => updates.some((n) => n.id === t.id)),
  );
  const previousTags = structuredClone(d.tags);
  const ops = plan(d, updates, tags);
  d.tasks = [
    ...d.tasks.filter((t) => !updates.some((n) => n.id === t.id)),
    ...updates,
  ];
  d.tags = tags;
  d.records = predict(d.records, ops);
  d.outbox.push(...ops);
  if (remember)
    d.undo = {
      before,
      after: structuredClone(updates),
      tagsBefore: previousTags,
      tagsAfter: structuredClone(tags),
    };
}
export function undo(d: Data) {
  const u = d.undo;
  if (!u) throw Error("undoUnavailable");
  const changed = u.after
    .filter((t) =>
      equal(
        t,
        d.tasks.find((n) => n.id === t.id),
      ),
    )
    .map(
      (t) =>
        u.before.find((n) => n.id === t.id) || {
          ...t,
          deleted: true,
          retracted: !!t.parentId,
        },
    );
  const tags = equal(d.tags, u.tagsAfter) ? u.tagsBefore : d.tags;
  if (!changed.length && equal(tags, d.tags)) throw Error("undoUnavailable");
  commit(d, changed, tags, false);
  delete d.undo;
}
export function project(d: Data, records: RecordValue[]) {
  d.records = predict(records, d.outbox);
  const tasks: Task[] = [],
    tags: Tag[] = [];
  for (const r of d.records) {
    const v = (key: string) => value(r, key);
    if (r.kind === "tag") {
      if (v("deleted") !== "true")
        tags.push({
          id: r.id,
          name: v("name"),
          color: v("color") || "#3877B8",
          icon: v("icon") || "tag",
        });
      continue;
    }
    const old = d.tasks.find((t) => t.id === r.id),
      t = {
        ...newTask(),
        id: r.id,
        title: v("title"),
        details: v("details"),
        completed: v("completed") === "true",
        priority: Number(v("priority")) || 0,
        dueDate: v("dueDate"),
        dueTime: v("dueDate") ? v("dueTime") : "",
        timeZone: v("timeZone"),
        checklist: JSON.parse(v("checklist") || "[]"),
        recurrence: v("dueDate") ? JSON.parse(v("recurrence") || "null") : null,
        tags: JSON.parse(v("tags") || "[]"),
        parentId: v("parentId"),
        deleted: v("deleted") === "true",
        retracted: v("retracted") === "true",
        createdAt: Number(v("createdAt")) || 0,
        rollover: r.fields.rollover
          ? v("rollover") === "true"
          : (old?.rollover ?? null),
      };
    tasks.push(
      old && equal(fields(old), fields(t))
        ? old
        : {
            ...t,
            generation: (old?.generation || 0) + 1,
            modifiedAt: Date.now(),
          },
    );
  }
  d.tasks = tasks;
  d.tags = tags;
}
export function conflicts(d: Data) {
  return d.records.flatMap((r) =>
    Object.entries(r.fields)
      .filter(([, vs]) => new Set(vs.map((v) => v.value)).size > 1)
      .map(([field, versions]) => ({ record: r, field, versions })),
  );
}
export function resolve(
  d: Data,
  id: string,
  field: string,
  selected: string,
  expected: string[],
) {
  const r = d.records.find((r) => r.id === id);
  if (
    !r ||
    !equal(r.fields[field].map((v) => v.id).sort(), [...expected].sort())
  )
    throw Error("syncChanged");
  const op: Operation = {
    id: uuid(),
    record: id,
    kind: r.kind,
    field,
    value: selected,
    replaces: expected,
    source: { id: d.source, name: "VerveTwoDo Web", platform: "linux" },
  };
  d.outbox.push(op);
  project(d, d.records);
}
export function importBackup(d: Data, text: string) {
  if (new TextEncoder().encode(text).length > 3 * 1024 * 1024)
    throw Error("invalidBackup");
  let b;
  try {
    b = JSON.parse(text);
  } catch {
    throw Error("invalidBackup");
  }
  if (
    b.format !== "vervetwodo-desktop" ||
    b.version !== 1 ||
    !Array.isArray(b.tasks) ||
    !Array.isArray(b.tags) ||
    new Set(b.tasks.map((t: Task) => t.id)).size !== b.tasks.length
  )
    throw Error("invalidBackup");
  const tags: Tag[] = [...d.tags];
  validateTags(b.tags.map((t: Tag) => ({ ...t, icon: t.icon || "tag" })));
  const map = new Map<string, string>();
  for (const t of b.tags) {
    let tag = tags.find(
      (n) => n.name.toLowerCase() === t.name.trim().toLowerCase(),
    );
    if (!tag) {
      tag = { ...t, id: uuid(), icon: t.icon || "tag" };
      tags.push(tag!);
    }
    map.set(t.id, tag!.id);
  }
  const tasks = b.tasks
    .filter((t: Task) => !t.deleted)
    .map((input: Task) => {
      const t = { ...newTask(), ...input };
      validateTask(t);
      if (t.tags.some((id: string) => !map.has(id)))
        throw Error("invalidBackup");
      return {
        ...t,
        id: uuid(),
        tags: t.tags.map((id: string) => map.get(id)!),
        parentId: "",
        generation: 0,
        retracted: false,
        createdAt: 0,
        modifiedAt: 0,
      };
    });
  commit(d, tasks, tags);
}
export function backup(d: Data) {
  return JSON.stringify(
    {
      format: "vervetwodo-desktop",
      version: 1,
      tasks: d.tasks.filter((t) => !t.deleted),
      tags: d.tags,
    },
    null,
    2,
  );
}
export function csv(d: Data) {
  const quote = (v: string) =>
    `"${(/^[=+@\-\t\r]/.test(v) ? "'" + v : v).replaceAll('"', '""')}"`;
  return [
    [
      "Title",
      "Completed",
      "Notes",
      "Tags",
      "Priority",
      "Date",
      "Time",
      "Timezone",
    ],
    ...d.tasks
      .filter((t) => !t.deleted)
      .map((t) => [
        t.title,
        String(t.completed),
        t.details,
        t.tags
          .map((id) => d.tags.find((t) => t.id === id)?.name || "")
          .join("; "),
        String(t.priority),
        t.dueDate,
        t.dueTime,
        t.timeZone,
      ]),
  ]
    .map((row) => row.map(quote).join(","))
    .join("\r\n");
}
export function sortTasks(tasks: Task[], sort: string) {
  const due = (a: Task, b: Task) =>
    (a.dueDate || "9999-99-99").localeCompare(b.dueDate || "9999-99-99") ||
    (a.dueTime || "99:99").localeCompare(b.dueTime || "99:99") ||
    b.priority - a.priority;
  return [...tasks].sort(
    (a, b) =>
      Number(a.completed) - Number(b.completed) ||
      (sort === "priority"
        ? b.priority - a.priority || due(a, b)
        : sort === "alphabetical"
          ? a.title.localeCompare(b.title)
          : sort === "newest"
            ? b.createdAt - a.createdAt
            : sort === "oldest"
              ? a.createdAt - b.createdAt
              : sort === "latest"
                ? a.dueDate && b.dueDate
                  ? b.dueDate.localeCompare(a.dueDate)
                  : due(a, b)
                : due(a, b)) ||
      a.id.localeCompare(b.id),
  );
}
export function groupKey(t: Task, day = today()) {
  if (!t.dueDate) return "noDate";
  if (t.dueDate < day) return t.completed ? "earlier" : "overdue";
  if (t.dueDate === day) return "today";
  if (t.dueDate === plusDays(day, 1)) return "tomorrow";
  const next = plusDays(day, 7 - ((dateUTC(day).getUTCDay() + 6) % 7));
  return t.dueDate < next
    ? "thisWeek"
    : t.dueDate < plusDays(next, 7)
      ? "nextWeek"
      : "later";
}
export function rollover(d: Data) {
  const changed = d.tasks
    .filter(
      (t) =>
        t.rollover &&
        !t.completed &&
        !t.deleted &&
        t.dueDate &&
        t.dueDate < today(),
    )
    .map((t) => ({ ...t, dueDate: today() }));
  if (changed.length) commit(d, changed, d.tags, false);
}
