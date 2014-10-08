package falcon.com.friendly.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import falcon.com.friendly.Util;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static falcon.com.friendly.store.FriendContract.FriendEntry;

/**
 * An IntentService for deleting friends from the friend database.
 */
public class DeleteFriendService extends IntentService {

  private static final String T = "DeleteFriendService";

  public DeleteFriendService() {
    super(T);
  }

  @Override
  protected void onHandleIntent(final Intent intent) {
    final long[] ids = intent.getLongArrayExtra("ids");
    if (deleteFriends(ids)) {
      Log.d(T, "Deleted " + ids.length + " friends");
    }
  }

  /**
   * Deletes the given friend ids from the database.
   *
   * @param ids the ids of the friends to delete
   * @return true if this invocation removed a value from the database, false otherwise
   */
  private boolean deleteFriends(final long[] ids) {
    final FriendlyDatabaseHelper databaseHelper = FriendlyDatabaseHelper.getInstance(this);
    final SQLiteDatabase db = databaseHelper.getWritableDatabase();

    final String whereClause = FriendEntry._ID + " IN " + Util.inClausePlaceholders(ids.length);
    final String[] whereArgs = new String[ids.length];
    for (int i = 0; i < ids.length; i++) {
      whereArgs[i] = String.valueOf(ids[i]);
    }

    Log.d(T, whereClause);
//    return db.delete(FriendEntry.TABLE, whereClause, whereArgs) > 0;
    return true;
  }
}
