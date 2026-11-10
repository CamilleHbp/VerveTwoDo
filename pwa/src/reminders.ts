import { Data, Task } from "./model";
import { mutate } from "./storage";
export function dueInstant(t: Task, defaultTime: string) {
  if (!t.dueDate) return NaN;
  const time = t.dueTime || defaultTime;
  const zone = t.dueTime
    ? t.timeZone
    : Intl.DateTimeFormat().resolvedOptions().timeZone;
  const target = Date.parse(`${t.dueDate}T${time}:00Z`);
  let epoch = target;
  const format = new Intl.DateTimeFormat("sv-SE", {
    timeZone: zone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  });
  for (let i = 0; i < 4; i++) {
    const parts = Object.fromEntries(
      format.formatToParts(new Date(epoch)).map((p) => [p.type, p.value]),
    );
    const represented = Date.parse(
      `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}:${parts.second}Z`,
    );
    const correction = target - represented;
    if (!correction) break;
    epoch += correction;
  }
  return epoch;
}
export async function remind(d: Data, body: string) {
  if (
    !d.preferences.reminders ||
    (d.preferences.push && d.connected) ||
    !("Notification" in window) ||
    Notification.permission !== "granted"
  )
    return;
  const registration = await navigator.serviceWorker?.getRegistration();
  if (!registration) return;
  const now = Date.now();
  for (const t of d.tasks.filter(
    (t) => !t.deleted && !t.completed && t.dueDate,
  )) {
    for (const offset of [...new Set([0, d.preferences.advance])]) {
      const time = dueInstant(t, d.preferences.reminderTime) - offset * 60000;
      const key = `${t.id}:${offset}`;
      if (time > now || time < now - 90000 || d.notified[key] === time)
        continue;
      let claimed = false;
      await mutate((current) => {
        if (current.notified[key] !== time) {
          current.notified[key] = time;
          claimed = true;
        }
      });
      if (claimed)
        await registration.showNotification(t.title, {
          body,
          tag: key,
          icon: "/icons/app-192.png",
          data: { taskId: t.id },
        });
    }
  }
}
