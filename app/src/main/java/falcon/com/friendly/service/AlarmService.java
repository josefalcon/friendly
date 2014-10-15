package falcon.com.friendly.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Calendar;

import falcon.com.friendly.receiver.AlarmReceiver;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

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

    final SQLiteDatabase db =
      FriendlyDatabaseHelper.getInstance(this).getReadableDatabase();

    final String query =
      "SELECT max(last_contact) as last_contact, min(frequency) as frequency FROM friend GROUP BY contact_id, lookup_key "
      + "ORDER BY (strftime('%s','now') - (last_contact / 1000)) / (1.0 * frequency) DESC LIMIT 1";

    final Cursor cursor = db.rawQuery(query, null);
    try {
      if (cursor.moveToNext()) {
        final long lastContact = cursor.getLong(cursor.getColumnIndex("last_contact"));
        final long frequency = cursor.getLong(cursor.getColumnIndex("frequency"));
        scheduleAlarm(lastContact + frequency);
      }
    } finally {
      cursor.close();
    }
  }

  /**
   * Schedules an alarm at the given time in milliseconds.
   *
   * @param timeInMillis the time to trigger the alarm
   */
  private void scheduleAlarm(final long timeInMillis) {
    final Calendar calendar = Calendar.getInstance();
    if (calendar.get(Calendar.HOUR_OF_DAY) > 17) {
      calendar.add(Calendar.DATE, 1);
    }

    calendar.set(Calendar.HOUR_OF_DAY, 17);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    final long triggerAtMillis = Math.max(calendar.getTimeInMillis(), timeInMillis);
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
