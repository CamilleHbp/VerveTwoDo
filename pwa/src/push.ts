import { Data } from "./model";
import { locale } from "./i18n";
import { api } from "./sync";
import { mutate } from "./storage";

export async function updatePush(data: Data, create = false) {
  if (!data.connected || !("PushManager" in window))
    throw Error("pushUnavailable");
  const reg = await navigator.serviceWorker.getRegistration();
  if (!reg) throw Error("pushUnavailable");
  let subscription = await reg.pushManager.getSubscription();
  if (!subscription && create) {
    const { key } = await api("push/key");
    const bytes = Uint8Array.from(
      atob(key.replaceAll("-", "+").replaceAll("_", "/")),
      (c) => c.charCodeAt(0),
    );
    subscription = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: bytes,
    });
  }
  if (!subscription) throw Error("pushUnavailable");
  const json = subscription.toJSON();
  await api("push/subscribe", {
    subscription: { endpoint: json.endpoint, keys: json.keys },
    locale: locale(data.preferences.language),
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    defaultTime: data.preferences.reminderTime,
    advance: data.preferences.advance,
  });
}
export async function enablePush(data: Data) {
  if (
    !("Notification" in window) ||
    (await Notification.requestPermission()) !== "granted"
  )
    throw Error("notificationError");
  await updatePush(data, true);
  await mutate((d) => {
    d.preferences.push = true;
    d.preferences.reminders = true;
  });
}
export async function disablePush() {
  await api("push/disable");
  const reg = await navigator.serviceWorker.getRegistration();
  await (await reg?.pushManager.getSubscription())?.unsubscribe();
  await mutate((d) => {
    d.preferences.push = false;
  });
}
