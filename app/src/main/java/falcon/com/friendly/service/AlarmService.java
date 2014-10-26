package falcon.com.friendly.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Calendar;

import falcon.com.friendly.Util;
import falcon.com.friendly.receiver.AlarmReceiver;
import falcon.com.friendly.store.FriendContract;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static falcon.com.friendly.store.FriendContract.*;

public class AlarmService extends IntentService {

  private static final String T = "AlarmService";

  private AlarmManager alarmManager;

  public AlarmService() {
    super(T);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
  }

  @Override
  protected void onHandleIntent(final Intent intent) {
    cancelExistingAlarm();

    final Cursor cursor = FriendlyDatabaseHelper.getInstance(this).listFriends(1);
    try {
      if (cursor.moveToNext()) {
        final long lastContact = cursor.getLong(cursor.getColumnIndex(FriendEntry.LAST_CONTACT));
        final long frequency = cursor.getLong(cursor.getColumnIndex(FriendEntry.FREQUENCY));
        final long nextTrigger = lastContact + frequency;
        long triggerAtMillis = getNextDefaultAlarmTime();
        if (lastContact > 0 && nextTrigger > System.currentTimeMillis()) {
          triggerAtMillis = lastContact + frequency;
        }
        scheduleAlarm(triggerAtMillis);
      }
    } finally {
      cursor.close();
    }
  }

  /**
   * Returns the next default alarm time. 5pm today, or tomorrow if it is later than 5pm.
   *
   * @return the next default alarm time
   */
  private long getNextDefaultAlarmTime() {
    final Calendar nextAlarm = Calendar.getInstance();
    Util.resetCalendarTime(nextAlarm);
    nextAlarm.set(Calendar.HOUR_OF_DAY, 17);

    final Calendar now = Calendar.getInstance();
    if (now.after(nextAlarm)) {
      nextAlarm.add(Calendar.DATE, 1);
    }

    return nextAlarm.getTimeInMillis();
  }

  /**
   * Schedules an alarm at the given time in milliseconds.
   *
   * @param triggerAtMillis the time to trigger the alarm
   */
  private void scheduleAlarm(final long triggerAtMillis) {
    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, makePendingIntent());
  }

  /**
   * Cancels an existing alarm if it exists.
   */
  private void cancelExistingAlarm() {
    alarmManager.cancel(makePendingIntent());
  }

  private static final int REQUEST_CODE = 0;

  private PendingIntent makePendingIntent() {
    final Intent intent = new Intent(this, AlarmReceiver.class);
    return PendingIntent.getBroadcast(this, REQUEST_CODE, intent,
                                      PendingIntent.FLAG_UPDATE_CURRENT);
  }
}
