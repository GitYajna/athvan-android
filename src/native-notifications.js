const STORAGE_KEY = "athvan.reminders.v1";

if (window.Capacitor?.isNativePlatform?.()) {
  startNativeNotifications().catch(error => {
    console.error("Athvan notifications failed:", error);
  });
}

async function startNativeNotifications() {
  const { LocalNotifications } =
    await import("@capacitor/local-notifications");

  let syncQueue = Promise.resolve();

  function notificationId(value, usedIds) {
    let hash = 2166136261;

    for (let i = 0; i < value.length; i++) {
      hash ^= value.charCodeAt(i);
      hash = Math.imul(hash, 16777619);
    }

    let id = (hash >>> 0) & 0x7fffffff;
    if (id === 0) id = 1;

    while (usedIds.has(id)) {
      id = id >= 2147483647 ? 1 : id + 1;
    }

    usedIds.add(id);
    return id;
  }

  async function hasPermission() {
    let permission = await LocalNotifications.checkPermissions();

    if (permission.display !== "granted") {
      permission = await LocalNotifications.requestPermissions();
    }

    return permission.display === "granted";
  }

  async function syncNotifications() {
    if (!(await hasPermission())) {
      console.warn("Athvan notification permission was not granted.");
      return;
    }

    let reminders = [];

    try {
      reminders = JSON.parse(
        localStorage.getItem(STORAGE_KEY) || "[]"
      );

      if (!Array.isArray(reminders)) reminders = [];
    } catch {
      reminders = [];
    }

    const pending = await LocalNotifications.getPending();

    if (pending.notifications.length) {
      await LocalNotifications.cancel({
        notifications: pending.notifications.map(item => ({
          id: item.id
        }))
      });
    }

    const usedIds = new Set();
    const notifications = [];
    const now = Date.now();

    for (const reminder of reminders) {
      if (
        !reminder ||
        !reminder.id ||
        !reminder.title ||
        !reminder.date ||
        !reminder.time ||
        reminder.completed
      ) {
        continue;
      }

      const scheduledTime = new Date(
        `${reminder.date}T${reminder.time}:00`
      );

      if (
        Number.isNaN(scheduledTime.getTime()) ||
        scheduledTime.getTime() <= now + 5000
      ) {
        continue;
      }

      notifications.push({
        id: notificationId(String(reminder.id), usedIds),
        title: reminder.title,
        body: reminder.notes || "Your reminder is due now.",
        schedule: {
          at: scheduledTime
        },
        extra: {
          reminderId: reminder.id
        }
      });
    }

    if (notifications.length) {
      const result = await LocalNotifications.schedule({
        notifications
      });

      if (result.warnings?.length) {
        console.warn("Athvan notification warnings:", result.warnings);
      }
    }
  }

  function queueSync() {
    syncQueue = syncQueue
      .catch(() => {})
      .then(syncNotifications)
      .catch(error => {
        console.error("Could not update Athvan notifications:", error);
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

  await syncNotifications();

  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) queueSync();
  });

  window.addEventListener("pageshow", queueSync);
}
