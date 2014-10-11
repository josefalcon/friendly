package falcon.com.friendly.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import falcon.com.friendly.MainActivity;

public class NotificationService extends IntentService {

  private static final String T = "NotificationService";

  private NotificationManager notificationManager;

  public NotificationService() {
    super(T);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
      .setContentTitle("Call your mom!")
      .setContentText("Make some calls")
      .setContentIntent(pendingIntent);

    notificationManager.notify(0, notificationBuilder.build());
  }
}
