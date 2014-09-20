package falcon.com.friendly.store;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static falcon.com.friendly.store.FriendContract.*;

public class FriendlyDatabaseHelper extends SQLiteOpenHelper {

  public static final int DATABASE_VERSION = 5;
  public static final String DATABASE_NAME = "Friendly";

  private static final String CREATE_FRIEND_ENTRY_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS "
    + FriendEntry.TABLE + " ("
    + FriendEntry._ID + " INTEGER PRIMARY KEY, "
    + FriendEntry.NUMBER + " TEXT, "
    + FriendEntry.CONTACT_ID + " INTEGER, "
    + FriendEntry.LOOKUP_KEY + " TEXT, "
    + FriendEntry.LAST_CONTACT + " INTEGER, "
    + "UNIQUE ("
    + FriendEntry.CONTACT_ID
    + ", "
    + FriendEntry.LOOKUP_KEY
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

}
