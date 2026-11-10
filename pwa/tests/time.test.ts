import { test } from "node:test";
import assert from "node:assert/strict";
import { composeTime, normalTime, timePartEdit } from "../src/time";

test("a full time field is overtyped by typed digits", () => {
  const one = timePartEdit("09", "091", 3, 23);
  assert.equal(one, "1");
  assert.equal(timePartEdit(one!, "10", 2, 23), "10");
  assert.equal(timePartEdit("09", "109", 1, 23), "1");
  assert.equal(timePartEdit("2", "25", 2, 23), "5");
  assert.equal(timePartEdit("5", "57", 2, 59), "57");
  assert.equal(timePartEdit("0", "", 0, 23), "");
  assert.equal(timePartEdit("09", "0a", 2, 23), null);
});

test("empty hour or minute takes the default reminder time", () => {
  assert.equal(composeTime("", "", "18:30"), "");
  assert.equal(composeTime("10", "", "18:30"), "10:30");
  assert.equal(composeTime("", "5", "18:30"), "18:05");
  assert.equal(composeTime("7", "", "bad"), "07:00");
  assert.equal(normalTime("24:00"), "09:00");
});
