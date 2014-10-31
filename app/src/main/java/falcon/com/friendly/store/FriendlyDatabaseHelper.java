package falcon.com.friendly.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static falcon.com.friendly.store.FriendContract.FriendEntry;
import static falcon.com.friendly.store.FriendContract.PhoneEntry;

public class FriendlyDatabaseHelper extends SQLiteOpenHelper {

  public static final String T = "FriendlyDatabaseHelper";

  public static final int DATABASE_VERSION = 15;

  public static final String DATABASE_NAME = "Friendly";

  private static final String CREATE_FRIEND_ENTRY_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS "
    + FriendEntry.TABLE + " ("
    + FriendEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
    + FriendEntry.CONTACT_ID + " INTEGER, "
    + FriendEntry.LOOKUP_KEY + " TEXT, "
    + FriendEntry.DISPLAY_NAME + " TEXT, "
    + FriendEntry.LAST_CONTACT + " INTEGER, "
    + FriendEntry.FREQUENCY + " INTEGER, "
    + "UNIQUE ("
    + FriendEntry.CONTACT_ID
    + ", "
    + FriendEntry.LOOKUP_KEY
    + "))";

  private static final String CREATE_PHONE_ENTRY_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS "
    + PhoneEntry.TABLE + " ("
    + PhoneEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
    + PhoneEntry.FRIEND_ID + " INTEGER, "
    + PhoneEntry.NUMBER + " TEXT, "
    + PhoneEntry.TYPE + " INTEGER, "
    + "UNIQUE ("
    + PhoneEntry.FRIEND_ID
    + ", "
    + PhoneEntry.NUMBER
    + ") "
    + "FOREIGN KEY(" + PhoneEntry.FRIEND_ID + ") "
    + "REFERENCES " + FriendEntry.TABLE + "(" + FriendEntry._ID + ")"
    + ")";

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
    db.execSQL(CREATE_PHONE_ENTRY_TABLE_SQL);
  }

  @Override
  public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    db.execSQL("DROP TABLE " + FriendEntry.TABLE);
    db.execSQL("DROP TABLE " + PhoneEntry.TABLE);
    onCreate(db);
  }

  /**
   * Creates a new friend in the database.
   *
   * @param friend the friend to create
   * @return true if the database was modified as a result of this invocation
   */
  public boolean createFriend(final Friend friend) {
    final SQLiteDatabase db = getWritableDatabase();

    final ContentValues cv = asContentValues(friend);
    try {
      db.beginTransaction();
      final long id = db.insert(FriendEntry.TABLE, null, cv);

      boolean success = id != -1;
      if (success && friend.numbers != null) {
        for (final Phone phone : friend.numbers) {
          if (createPhoneForFriend(phone, id)) {
            // any failure to create a friend should cancel the transaction
            success = false;
            break;
          }
        }
      }

      if (success) {
        db.setTransactionSuccessful();
      }
      return success;
    } finally {
      db.endTransaction();
    }
  }

  /**
   * Persists the given phone for the given friend id.
   *
   * @param phone     the phone to persist
   * @param friendId  the owner of the phone
   * @return true if the invocation of this method modified the database, false otherwise
   */
  private boolean createPhoneForFriend(final Phone phone, final long friendId) {
    final SQLiteDatabase db = getWritableDatabase();

    final ContentValues cv = new ContentValues();
    cv.put(PhoneEntry.FRIEND_ID, friendId);
    cv.put(PhoneEntry.NUMBER, phone.number);
    cv.put(PhoneEntry.TYPE, phone.type);
    return db.insert(PhoneEntry.TABLE, null, cv) == -1;
  }

  /**
   * Returns the Friend associated with the given contact id and lookup key.
   *
   * @param contactId the contact id
   * @param lookupKey the contact lookup key
   * @return the Friend associated with the given contact id and lookup key, or null.
   */
  public Friend getFriend(final long contactId, final String lookupKey) {
    final SQLiteDatabase db = getReadableDatabase();

    final String query =
      "SELECT f.*, p._id as phone_id, p.number, p.type FROM friend as f "
      + "JOIN phone as p ON p.friend_id = f._id "
      + "WHERE f.contact_id = ? "
      + "AND f.lookup_key = ?";

    final Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(contactId), lookupKey });

    Friend friend = null;
    try {
      while (cursor.moveToNext()) {
        if (friend == null) {
          friend = new Friend();
          friend.id = cursor.getLong(cursor.getColumnIndex(FriendEntry._ID));
          friend.contactId = contactId;
          friend.lookupKey = lookupKey;
          friend.displayName = cursor.getString(cursor.getColumnIndex(FriendEntry.DISPLAY_NAME));
          friend.lastContact = cursor.getLong(cursor.getColumnIndex(FriendEntry.LAST_CONTACT));
          friend.frequency = cursor.getLong(cursor.getColumnIndex(FriendEntry.FREQUENCY));
          friend.numbers = new ArrayList<>(cursor.getCount());
        }

        final Phone phone = new Phone();
        phone.id = cursor.getLong(cursor.getColumnIndex("phone_id"));
        phone.number = cursor.getString(cursor.getColumnIndex(PhoneEntry.NUMBER));
        phone.type = cursor.getInt(cursor.getColumnIndex(PhoneEntry.TYPE));
        friend.numbers.add(phone);
      }
    } finally {
      cursor.close();
    }
    return friend;
  }

  /**
   * Updates the given friend.
   *
   * @param friend the friend to update or create.
   * @return true if this invocation modified the database, false otherwise
   */
  public boolean updateFriend(final Friend friend) {
    final SQLiteDatabase db = getWritableDatabase();

    final ContentValues cv = asContentValues(friend);
    try {
      db.beginTransaction();
      final String whereClause = FriendEntry._ID + " = ?";
      final String[] whereArgs = new String[] { String.valueOf(friend.id) };

      final int count = db.update(FriendEntry.TABLE, cv, whereClause, whereArgs);
      boolean success = count == 1;
      if (success && friend.numbers != null) {
        for (final Phone phone : friend.numbers) {
          if (phone.id == null) {
            if (createPhoneForFriend(phone, friend.id)) {
              // any failure to create a friend should cancel the transaction
              success = false;
              break;
            }
          }
        }
      }

      if (success) {
        db.setTransactionSuccessful();
      }
      return success;
    } finally {
      db.endTransaction();
    }
  }

  /**
   * Deletes the given friend id from the database, along with any associated phone numbers.
   *
   * @param id the id of the friend to delete
   * @return true if this invocation removed a value from the database, false otherwise
   */
  public boolean deleteFriend(final long id) {
    final SQLiteDatabase db = getWritableDatabase();

    final String[] whereArgs = new String[] { String.valueOf(id) };
    try {
      db.beginTransaction();
      int deleted = 0;
      deleted += db.delete(PhoneEntry.TABLE, PhoneEntry.FRIEND_ID + " = ?", whereArgs);
      deleted += db.delete(FriendEntry.TABLE, FriendEntry._ID + " = ?", whereArgs);
      db.setTransactionSuccessful();
      return deleted > 0;
    } finally {
      db.endTransaction();
    }
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
      "SELECT *"
      + "FROM friend "
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

  /**
   * Returns the phone numbers associated with the given friend id, or an empty list.
   *
   * @param friendId the owner of the phone
   * @return a list of phone numbers
   */
  public ArrayList<Phone> getPhoneNumbers(final long friendId) {
    final SQLiteDatabase db = getReadableDatabase();

    final String[] whereArgs = new String[] { String.valueOf(friendId) };
    final String whereClause = PhoneEntry.FRIEND_ID + " = ?";
    final Cursor cursor =
      db.query(PhoneEntry.TABLE, null, whereClause, whereArgs, null, null, null);

    final ArrayList<Phone> numbers = new ArrayList<>();
    try {
      while (cursor.moveToNext()) {
        final Phone phone = new Phone();
        phone.id = cursor.getLong(cursor.getColumnIndex(PhoneEntry._ID));
        phone.number = cursor.getString(cursor.getColumnIndex(PhoneEntry.NUMBER));
        phone.type = cursor.getInt(cursor.getColumnIndex(PhoneEntry.TYPE));
        numbers.add(phone);
      }
    } finally {
      cursor.close();
    }
    return numbers;
  }

  /**
   * Converts the given friend to ContentValues, suitable for SQL calls. Does not include phone
   * values in the returned map.
   *
   * @param friend the friend to convert
   * @return new ContentValues
   */
  private static ContentValues asContentValues(final Friend friend) {
    final ContentValues cv = new ContentValues();
    cv.put(FriendEntry.CONTACT_ID, friend.contactId);
    cv.put(FriendEntry.LOOKUP_KEY, friend.lookupKey);
    cv.put(FriendEntry.DISPLAY_NAME, friend.displayName);
    cv.put(FriendEntry.LAST_CONTACT, friend.lastContact);
    cv.put(FriendEntry.FREQUENCY, friend.frequency);

    if (friend.id != null) {
      cv.put(FriendEntry._ID, friend.id);
    }
    return cv;
  }

}
