import { test } from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import {
  initial,
  newTask,
  commit,
  completion,
  nextDate,
  successorID,
  undo,
  backup,
  importBackup,
  project,
  conflicts,
  resolve,
  rollover,
  validateTask,
  csv,
  uuid,
} from "../src/model";
import { dueInstant } from "../src/reminders";
import en from "../src/en.json";
import fr from "../src/fr.json";
const task = () => ({ ...newTask(), title: "A task", dueDate: "2026-01-31" });
test("calendar recurrence retains month-end and leap-day anchors", () => {
  const r = {
    frequency: "monthly" as const,
    interval: 1,
    anchor: "2026-01-31",
  };
  assert.equal(nextDate("2026-01-31", r), "2026-02-28");
  assert.equal(nextDate("2026-02-28", r), "2026-03-31");
  assert.equal(
    nextDate("2027-02-28", {
      frequency: "yearly",
      interval: 1,
      anchor: "2024-02-29",
    }),
    "2028-02-29",
  );
  assert.throws(() => nextDate("9999-12-31", r));
});
test("recurrence UUID matches Java no-namespace MD5", () => {
  const id = uuid(),
    date = "2026-02-28",
    b = createHash("md5").update(`${id}:${date}`).digest();
  b[6] = (b[6] & 15) | 48;
  b[8] = (b[8] & 63) | 128;
  assert.equal(successorID(id, date).replaceAll("-", ""), b.toString("hex"));
});
test("completion is idempotent, resets steps, and undo retracts the successor", () => {
  const d = initial(),
    t = {
      ...task(),
      recurrence: {
        frequency: "monthly" as const,
        interval: 1,
        anchor: "2026-01-31",
      },
      checklist: [{ id: uuid(), title: "Step", completed: true }],
    };
  commit(d, [t]);
  commit(d, completion(d, [t.id], true));
  const next = d.tasks.find((n) => n.parentId === t.id)!;
  assert.equal(next.dueDate, "2026-02-28");
  assert.equal(next.checklist[0].completed, false);
  assert.equal(completion(d, [t.id], true).length, 0);
  undo(d);
  assert.equal(d.tasks.find((n) => n.id === t.id)?.completed, false);
  assert.equal(d.tasks.find((n) => n.id === next.id)?.retracted, true);
  commit(d, completion(d, [t.id], true));
  assert.equal(d.tasks.filter((n) => !n.deleted).length, 2);
});
test("reopening preserves an edited successor", () => {
  const d = initial(),
    t = {
      ...task(),
      recurrence: {
        frequency: "daily" as const,
        interval: 1,
        anchor: "2026-01-31",
      },
    };
  commit(d, [t]);
  commit(d, completion(d, [t.id], true));
  const child = d.tasks.find((t) => t.parentId)!;
  commit(d, [{ ...child, title: "Keep me" }]);
  commit(d, completion(d, [t.id], false));
  assert.equal(d.tasks.find((t) => t.id === child.id)?.deleted, false);
});
test("undo preserves later unrelated edits", () => {
  const d = initial(),
    a = task(),
    b = task();
  commit(d, [a, b]);
  commit(d, [{ ...d.tasks[0], deleted: true }]);
  const u = structuredClone(d.undo);
  commit(d, [{ ...d.tasks.find((t) => t.id === b.id)!, title: "Later" }]);
  d.undo = u;
  undo(d);
  assert.equal(d.tasks.find((t) => t.id === a.id)?.deleted, false);
  assert.equal(d.tasks.find((t) => t.id === b.id)?.title, "Later");
});
test("offline field edits merge and competing edits remain resolvable", () => {
  const base = initial(),
    t = task();
  commit(base, [t]);
  base.outbox = [];
  const a = structuredClone(base),
    b = structuredClone(base);
  commit(a, [{ ...a.tasks[0], title: "Browser A" }]);
  commit(b, [{ ...b.tasks[0], title: "Browser B", details: "independent" }]);
  project(a, b.records);
  assert.equal(a.tasks[0].details, "independent");
  assert.equal(conflicts(a).length, 1);
  const issue = conflicts(a)[0];
  resolve(
    a,
    issue.record.id,
    issue.field,
    "Browser B",
    issue.versions.map((v) => v.id),
  );
  assert.equal(conflicts(a).length, 0);
  assert.equal(a.tasks[0].title, "Browser B");
  assert.throws(() =>
    resolve(
      a,
      t.id,
      "title",
      "stale",
      issue.versions.map((v) => v.id),
    ),
  );
});
test("backup imports create fresh identities and merge tag names", () => {
  const d = initial(),
    tag = { id: uuid(), name: "Work", color: "#3877B8", icon: "work" },
    t = { ...task(), tags: [tag.id] };
  commit(d, [t], [tag]);
  const b = backup(d);
  importBackup(d, b);
  assert.equal(d.tasks.length, 2);
  assert.notEqual(d.tasks[0].id, d.tasks[1].id);
  assert.equal(d.tags.length, 1);
  importBackup(d, b);
  assert.equal(d.tasks.length, 3);
  assert.throws(() =>
    importBackup(
      d,
      JSON.stringify({
        format: "vervetwodo-desktop",
        version: 1,
        tasks: [{ ...t, tags: ["unknown"] }],
        tags: [],
      }),
    ),
  );
});
test("rollover preserves time and excludes completed tasks", () => {
  const d = initial(),
    t = {
      ...task(),
      rollover: true,
      dueTime: "09:00",
      timeZone: "Europe/Paris",
    };
  commit(d, [t, { ...task(), completed: true, rollover: true }]);
  rollover(d);
  assert.notEqual(d.tasks.find((n) => n.id === t.id)?.dueDate, "2026-01-31");
  assert.equal(d.tasks.find((n) => n.id === t.id)?.dueTime, "09:00");
  assert.equal(d.tasks.find((n) => n.completed)?.dueDate, "2026-01-31");
});
test("validation rejects invalid dates, timezones, checklist and recurrence", () => {
  assert.throws(() => validateTask({ ...task(), dueDate: "2026-02-30" }));
  assert.throws(() =>
    validateTask({ ...task(), dueTime: "10:00", timeZone: "Not/AZone" }),
  );
  assert.throws(() =>
    validateTask({
      ...task(),
      checklist: [{ id: "a", title: "", completed: false }],
    }),
  );
  assert.throws(() =>
    validateTask({
      ...task(),
      recurrence: { frequency: "daily", interval: 0, anchor: "2026-01-31" },
    }),
  );
});
test("timed reminders honor explicit timezones and daylight saving", () => {
  assert.equal(
    new Date(
      dueInstant(
        {
          ...task(),
          dueDate: "2026-07-01",
          dueTime: "09:00",
          timeZone: "Europe/Paris",
        },
        "09:00",
      ),
    ).toISOString(),
    "2026-07-01T07:00:00.000Z",
  );
  assert.equal(
    new Date(
      dueInstant(
        {
          ...task(),
          dueDate: "2026-12-01",
          dueTime: "09:00",
          timeZone: "Europe/Paris",
        },
        "09:00",
      ),
    ).toISOString(),
    "2026-12-01T08:00:00.000Z",
  );
});
test("CSV neutralizes spreadsheet formula injection", () => {
  const d = initial();
  commit(d, [{ ...task(), title: '=WEBSERVICE("x")' }]);
  assert.ok(csv(d).includes(`"'=WEBSERVICE(""x"")"`));
});
test("English and French keys match", () =>
  assert.deepEqual(Object.keys(en).sort(), Object.keys(fr).sort()));
