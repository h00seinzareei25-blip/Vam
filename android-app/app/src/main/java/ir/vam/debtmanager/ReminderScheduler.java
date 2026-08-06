package ir.vam.debtmanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class ReminderScheduler {
    private static final String TAG = "ReminderScheduler";
    private static final String PREFS = "reminder_prefs";
    private static final String KEY_REMINDERS = "reminders_json";
    private static final String KEY_IDS = "alarm_ids";
    private static final int MAX_ALARMS = 80;
    private static final int HOUR = 9;
    private static final int MINUTE = 0;

    private final Context context;
    private final AlarmManager alarmManager;
    private final SharedPreferences prefs;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void scheduleFromJson(String remindersJson) {
        cancelAll();
        if (remindersJson == null || remindersJson.trim().isEmpty() || remindersJson.equals("[]")) {
            prefs.edit().putString(KEY_REMINDERS, "[]").apply();
            return;
        }

        try {
            JSONArray array = new JSONArray(remindersJson);
            prefs.edit().putString(KEY_REMINDERS, remindersJson).apply();

            Set<String> ids = new HashSet<>();
            long now = System.currentTimeMillis();
            int scheduled = 0;

            for (int i = 0; i < array.length() && scheduled < MAX_ALARMS; i++) {
                JSONObject item = array.getJSONObject(i);
                int id = item.optInt("id", i + 1);
                String title = item.optString("title", "یادآوری قسط");
                String body = item.optString("body", "قسطی نزدیک به سررسید دارید");
                int year = item.getInt("year");
                int month = item.getInt("month"); // 1-12 Gregorian
                int day = item.getInt("day");
                int daysBefore = item.optInt("daysBefore", 0);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month - 1);
                cal.set(Calendar.DAY_OF_MONTH, day);
                cal.set(Calendar.HOUR_OF_DAY, HOUR);
                cal.set(Calendar.MINUTE, MINUTE);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                if (daysBefore > 0) {
                    cal.add(Calendar.DAY_OF_MONTH, -daysBefore);
                }

                long triggerAt = cal.getTimeInMillis();
                if (triggerAt <= now) {
                    continue;
                }

                scheduleAlarm(id, title, body, triggerAt);
                ids.add(String.valueOf(id));
                scheduled++;
            }

            prefs.edit().putStringSet(KEY_IDS, ids).apply();
            Log.i(TAG, "Scheduled " + scheduled + " reminders");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule reminders", e);
        }
    }

    public void rescheduleSaved() {
        String json = prefs.getString(KEY_REMINDERS, "[]");
        scheduleFromJson(json);
    }

    public void cancelAll() {
        Set<String> ids = prefs.getStringSet(KEY_IDS, new HashSet<>());
        if (ids != null) {
            for (String idStr : ids) {
                try {
                    int id = Integer.parseInt(idStr);
                    PendingIntent pi = buildPendingIntent(
                            id,
                            "",
                            "",
                            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                    );
                    if (pi != null) {
                        alarmManager.cancel(pi);
                        pi.cancel();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        prefs.edit().remove(KEY_IDS).apply();
    }

    private void scheduleAlarm(int id, String title, String body, long triggerAt) {
        PendingIntent pi = buildPendingIntent(id, title, body,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (pi == null || alarmManager == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException se) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private PendingIntent buildPendingIntent(int id, String title, String body, int flags) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("ir.vam.debtmanager.REMINDER_" + id);
        intent.putExtra(ReminderReceiver.EXTRA_ID, id);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_BODY, body);
        int finalFlags = flags;
        if ((finalFlags & PendingIntent.FLAG_IMMUTABLE) == 0
                && (finalFlags & PendingIntent.FLAG_MUTABLE) == 0) {
            finalFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, id, intent, finalFlags);
    }
}
