package falcon.com.friendly.store;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import falcon.com.friendly.service.AlarmService;

import static falcon.com.friendly.store.FriendContract.FriendEntry;

public class FriendlyDatabaseHelper extends SQLiteOpenHelper {

  public static final String T = "FriendlyDatabaseHelper";

  public static final int DATABASE_VERSION = 10;

  public static final String DATABASE_NAME = "Friendly";

  private static final String CREATE_FRIEND_ENTRY_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS "
    + FriendEntry.TABLE + " ("
    + FriendEntry._ID + " INTEGER PRIMARY KEY, "
    + FriendEntry.NUMBER + " TEXT, "
    + FriendEntry.TYPE + " INTEGER, "
    + FriendEntry.CONTACT_ID + " INTEGER, "
    + FriendEntry.LOOKUP_KEY + " TEXT, "
    + FriendEntry.LAST_CONTACT + " INTEGER, "
    + FriendEntry.FREQUENCY + " INTEGER, "
    + "UNIQUE ("
    + FriendEntry.NUMBER
    + "))";

  private static FriendlyDatabaseHelper singleton;

  private FriendlyDatabaseHelper(final Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  public static FriendlyDatabaseHelper getInstance(final Context context) {
    if (singleton == null) {
      singleton = new FriendlyDatabaseHelper(context);
    }
    return singleton;
  }

  @Override
  public void onCreate(final SQLiteDatabase db) {
    db.execSQL(CREATE_FRIEND_ENTRY_TABLE_SQL);
  }

  @Override
  public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    db.execSQL("DROP TABLE " + FriendEntry.TABLE);
    onCreate(db);
  }

  /**
   * Updates or creates the given friend.
   *
   * @param contentValues the friend to update or create.
   * @return true if this invocation modified the database, false otherwise
   */
  public boolean updateOrCreate(final ContentValues contentValues) {
    final SQLiteDatabase db = getWritableDatabase();
    try {
      final long id = db.replaceOrThrow(FriendContract.FriendEntry.TABLE, null, contentValues);
      return id != -1;
    } catch (final SQLException e) {
      Log.d(T, "Ignoring duplicate friend");
    }
    return false;
  }

  /**
   * Deletes the given friend id from the database.
   *
   * @param id the id of the friend to delete
   * @return true if this invocation removed a value from the database, false otherwise
   */
  public boolean deleteFriend(final long id) {
    final SQLiteDatabase db = getWritableDatabase();
    final String whereClause = FriendEntry._ID + " = ?";
    final String[] whereArgs = new String[] { String.valueOf(id) };
    return db.delete(FriendEntry.TABLE, whereClause, whereArgs) > 0;
  }

  /**
   * Returns a list of unique friends ordered by contact freshness.
   *
   * @param limit the number of friends to limit. pass null for no limit.
   * @return a cursor of unique friends ordered by contact freshness
   */
  public Cursor listFriends(final Integer limit) {
    final SQLiteDatabase db = getReadableDatabase();
    String query =
      "SELECT *, max(last_contact), min(frequency) "
      + "FROM friend "
      + "GROUP BY contact_id, lookup_key "
      + "ORDER BY "
      + "(CAST(strftime('%s','now') as integer) - (last_contact / 1000)) / (1.0 * frequency) DESC";
    if (limit != null) {
      query += " LIMIT " + limit;
    }
    return db.rawQuery(query, null);
  }

  /**
   * Returns the total number of friends that need to be contacted.
   *
   * @return the total number of friends that need to be contacted
   */
  public int countOffendingFriends() {
    final SQLiteDatabase db = getReadableDatabase();

    final String query =
      "SELECT * FROM friend "
      + "WHERE ((last_contact + frequency) / 1000) < CAST(strftime('%s','now') as integer) "
      + "GROUP BY contact_id, lookup_key";

    final Cursor cursor = db.rawQuery(query, null);
    try {
      return cursor.getCount();
    } finally {
      cursor.close();
    }
  }

}
