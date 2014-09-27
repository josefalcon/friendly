package falcon.com.friendly.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import falcon.com.friendly.resolver.CallLogResolver;
import falcon.com.friendly.store.FriendContract;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

public class CallLogUpdateService extends IntentService {

  private static final String T = "CallLogUpdateService";

  public CallLogUpdateService() {
    super("CallLogUpdateService");
  }

  @Override
  protected void onHandleIntent(final Intent intent) {
    Log.d(T, "Requested to update last contact");

    final CallLogResolver callLogResolver = new CallLogResolver(getContentResolver());

    final SQLiteDatabase db =
      FriendlyDatabaseHelper.getInstance(this).getWritableDatabase();
    final Cursor cursor =
      db.query(FriendContract.FriendEntry.TABLE, null, null, null, null, null, null);

    try {
      while (cursor.moveToNext()) {
        final long lastContact =
          cursor.getLong(cursor.getColumnIndex(FriendContract.FriendEntry.LAST_CONTACT));
        final String number =
          cursor.getString(cursor.getColumnIndex(FriendContract.FriendEntry.NUMBER));

        final long latestContact = callLogResolver.getLastContact(number);
        if (latestContact > lastContact) {
          final long id = cursor.getLong(cursor.getColumnIndex(FriendContract.FriendEntry._ID));

          Log.d(T, "Updating last contact for " + id);
          final ContentValues contentValues = new ContentValues();
          contentValues.put(FriendContract.FriendEntry.LAST_CONTACT, latestContact);
          db.update(FriendContract.FriendEntry.TABLE,
                    contentValues,
                    FriendContract.FriendEntry._ID + " = ?",
                    new String[]{String.valueOf(id)});
        }
      }
    } finally {
      cursor.close();
    }
  }

}
