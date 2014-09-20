package falcon.com.friendly;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import falcon.com.friendly.store.FriendContract;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static android.provider.ContactsContract.CommonDataKinds.Phone.*;


public class NewFriendActivity extends Activity {

  private static final String T = "NewFriendActivity";

  private static final int PICK_CONTACT_REQUEST = 1;

  private ContentValues contentValues;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_new_friend);

    final Intent pickContactIntent =
      new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    pickContactIntent.setType(CONTENT_TYPE);
    startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
  }

  public static final String[] PROJECTION = new String[] {
    CONTACT_ID,
    LOOKUP_KEY,
    NUMBER,
  };

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
      final Uri contactUri = data.getData();
      final Cursor cursor = getContentResolver().query(contactUri, PROJECTION, null, null, null);
      try {
        if (cursor.moveToFirst()) {
          final long contactId = cursor.getLong(cursor.getColumnIndex(CONTACT_ID));
          final String lookupKey = cursor.getString(cursor.getColumnIndex(LOOKUP_KEY));
          final String phoneNumber =
            cleanPhoneNumber(cursor.getString(cursor.getColumnIndex(NUMBER)));

          contentValues = new ContentValues();
          contentValues.put(FriendContract.FriendEntry.CONTACT_ID, contactId);
          contentValues.put(FriendContract.FriendEntry.LOOKUP_KEY, lookupKey);
          contentValues.put(FriendContract.FriendEntry.NUMBER, phoneNumber);
        }
      } finally {
        cursor.close();
      }
    } else {
      finish();
    }
  }

  public void saveAndFinish(final View view) {
    int result = RESULT_CANCELED;
    if (contentValues != null) {
      if (saveFriend(contentValues)) {
        result = RESULT_OK;
      }
    }
    setResult(result);
    finish();
  }

  /**
   * Persists the given ContentValues to the Friend database table.
   *
   * @param contentValues the ContentValues representing a Friend.
   */
  private boolean saveFriend(final ContentValues contentValues) {
    Log.d(T, "Saving friend...");
    final FriendlyDatabaseHelper helper = FriendlyDatabaseHelper.getInstance(this);
    final SQLiteDatabase db = helper.getWritableDatabase();
    try {
      final long id = db.insertOrThrow(FriendContract.FriendEntry.TABLE, null, contentValues);
      return id != -1;
    } catch (final SQLException e) {
      Log.d(T, "Ignoring duplicate friend...");
    }
    Log.d(T, "Done");
    return false;
  }


  /**
   * Strips separators and formats the given phone number into a canonical form.
   *
   * @param rawNumber the raw number to clean
   * @return a canonical phone number
   */
  private String cleanPhoneNumber(final String rawNumber) {
    return PhoneNumberUtils.stripSeparators(PhoneNumberUtils.formatNumber(rawNumber));
  }
}
