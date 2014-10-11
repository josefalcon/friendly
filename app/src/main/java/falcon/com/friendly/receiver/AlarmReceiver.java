package falcon.com.friendly.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import falcon.com.friendly.service.NotificationService;

public class AlarmReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(final Context context, final Intent intent) {
    context.startService(new Intent(context, NotificationService.class));
  }

}
