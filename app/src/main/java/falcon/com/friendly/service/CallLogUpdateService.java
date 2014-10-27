package falcon.com.friendly.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import falcon.com.friendly.resolver.CallLogResolver;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static falcon.com.friendly.store.FriendContract.FriendEntry;
import static falcon.com.friendly.store.FriendContract.PhoneEntry;

/**
 * An IntentService for updating the Friend database with recent call log activity.
 */
public class CallLogUpdateService extends IntentService {

  private static final String T = "CallLogUpdateService";

  public CallLogUpdateService() {
    super(T);
  }

  @Override
  protected void onHandleIntent(final Intent intent) {
    final CallLogResolver callLogResolver = new CallLogResolver(getContentResolver());
    final SQLiteDatabase db =
      FriendlyDatabaseHelper.getInstance(this).getWritableDatabase();

    final Cursor cursor =
      db.query(PhoneEntry.TABLE, null, null, null, null, null, null);

    final Map<Long, Long> friendIdToLastContact = new HashMap<>();
    try {
      while (cursor.moveToNext()) {
        final long friendId =
          cursor.getLong(cursor.getColumnIndex(PhoneEntry.FRIEND_ID));
        final String number =
          cursor.getString(cursor.getColumnIndex(PhoneEntry.NUMBER));

        final long latestContact = callLogResolver.getLastContact(number);
        final Long lastContact = friendIdToLastContact.get(friendId);
        if (lastContact == null || latestContact > lastContact) {
          friendIdToLastContact.put(friendId, latestContact);
        }
      }
    } finally {
      cursor.close();
    }

    for (final Map.Entry<Long, Long> entry : friendIdToLastContact.entrySet()) {
      final Long friendId = entry.getKey();
      final Long latestContact = entry.getValue();

      if (latestContact != -1) {
        Log.d(T, "Updating last contact for " + friendId);
        final ContentValues contentValues = new ContentValues();
        contentValues.put(FriendEntry.LAST_CONTACT, latestContact);
        db.update(FriendEntry.TABLE,
                  contentValues,
                  FriendEntry._ID + " = ?",
                  new String[]{String.valueOf(friendId)});
      }
    }

  }

}
