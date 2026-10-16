
import { registerPlugin } from "@capacitor/core";

const STORAGE_KEY = "athvan.reminders.v1";
const AthvanAlarm = registerPlugin("AthvanAlarm");

if (window.Capacitor?.isNativePlatform?.()) {
  startNativeAlarms().catch(error => {
    console.error("Athvan alarm setup failed:", error);
  });
}

async function startNativeAlarms() {
  const { LocalNotifications } =
    await import("@capacitor/local-notifications");

  let syncQueue = Promise.resolve();

  async function ensurePermissions() {
    let permission = await LocalNotifications.checkPermissions();

    if (permission.display !== "granted") {
      permission = await LocalNotifications.requestPermissions();
    }

    if (permission.display !== "granted") {
      console.warn("Allow Athvan notifications in Android Settings.");
      return false;
    }

    let exact = await LocalNotifications.checkExactNotificationSetting();

    if (exact.exact_alarm !== "granted") {
      await LocalNotifications.changeExactNotificationSetting();
      exact = await LocalNotifications.checkExactNotificationSetting();
    }

    if (exact.exact_alarm !== "granted") {
      console.warn("Enable Alarms and reminders for Athvan.");
      return false;
    }

    return true;
  }

  async function syncAlarms() {
    if (!(await ensurePermissions())) return;

    let reminders = [];

    try {
      reminders = JSON.parse(
        localStorage.getItem(STORAGE_KEY) || "[]"
      );

      if (!Array.isArray(reminders)) reminders = [];
    } catch {
      reminders = [];
    }

    const result = await AthvanAlarm.sync({ reminders });

    console.log(
      `Athvan native alarms scheduled: ${result.scheduled}`
    );
  }

  function queueSync() {
    syncQueue = syncQueue
      .catch(() => {})
      .then(syncAlarms)
      .catch(error => {
        console.error("Could not sync Athvan alarms:", error);
      });
  }

  const originalSetItem = Storage.prototype.setItem;

  Storage.prototype.setItem = function (key, value) {
    const result = originalSetItem.call(this, key, value);

    if (this === window.localStorage && key === STORAGE_KEY) {
      queueSync();
    }

    return result;
  };

  await syncAlarms();

  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) queueSync();
  });

  window.addEventListener("pageshow", queueSync);
}
