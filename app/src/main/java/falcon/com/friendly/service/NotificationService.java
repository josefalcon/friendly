package falcon.com.friendly.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import falcon.com.friendly.MainActivity;
import falcon.com.friendly.resolver.ContactResolver;
import falcon.com.friendly.store.FriendContract;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

public class NotificationService extends IntentService {

  private static final String T = "NotificationService";

  private NotificationManager notificationManager;

  private ContactResolver contactResolver;

  public NotificationService() {
    super(T);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    contactResolver = new ContactResolver(getApplicationContext().getContentResolver());
  }

  @Override
  protected void onHandleIntent(final Intent intent) {
    Log.d(T, "notify!");

    final Intent mainIntent = new Intent(this.getApplicationContext(), MainActivity.class);
    final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    stackBuilder.addParentStack(MainActivity.class);
    stackBuilder.addNextIntent(mainIntent);

    final PendingIntent pendingIntent =
      stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    final Notification.Builder notificationBuilder = new Notification.Builder(this)
      .setOnlyAlertOnce(true)
      .setSmallIcon(android.R.drawable.ic_menu_call)
      .setDefaults(Notification.DEFAULT_ALL)
      .setAutoCancel(false)
      .setOngoing(false)
      .setContentTitle("Call your friends")
      .setContentText(getContentText())
      .setContentIntent(pendingIntent);

    notificationManager.notify(0, notificationBuilder.build());

    // schedule next alarm
    final Context context = getApplicationContext();
    context.startService(new Intent(context, AlarmService.class));
  }

  /**
   * Returns the text to display in the alarm.
   *
   * @return text to display in the alarm
   */
  private String getContentText() {
    final int count = FriendlyDatabaseHelper.getInstance(this).countOffendingFriends();
    String friends = "friend";
    if (count != 1) {
      friends += "s";
    }

    return String.format("You've got %s %s to call!", count, friends);
  }
}
