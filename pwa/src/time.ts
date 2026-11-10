const valid = /^([01]\d|2[0-3]):[0-5]\d$/;

/** The settings default as HH:MM, falling back to 09:00 when it is missing or malformed. */
export function normalTime(value: string): string {
  return valid.test(value) ? value : "09:00";
}

/**
 * Interprets an edit of a two-digit hour or minute field. A full field is
 * overtyped by the newly typed digit, so typing 1 then 0 over 09 gives 10
 * wherever the caret is. Returns null when the edit should be ignored.
 */
export function timePartEdit(
  previous: string,
  next: string,
  caret: number | null,
  max: number,
): string | null {
  if (!/^\d*$/.test(next)) return null;
  if (next.length <= 2 && (next === "" || Number(next) <= max)) return next;
  if (next.length <= previous.length) return null;
  const typed = next[(caret ?? next.length) - 1];
  return typed !== undefined && Number(typed) <= max ? typed : null;
}

/** Combines hour and minute drafts; an empty part takes the default time's part. */
export function composeTime(
  hour: string,
  minute: string,
  fallback: string,
): string {
  if (!hour && !minute) return "";
  const [h, m] = normalTime(fallback).split(":");
  return `${(hour || h).padStart(2, "0")}:${(minute || m).padStart(2, "0")}`;
}
