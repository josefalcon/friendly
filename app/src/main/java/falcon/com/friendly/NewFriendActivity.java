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


public class NewFriendActivity extends Activity {

  private static final String T = "NewFriendActivity";

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_new_friend);
    pickContact();
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.new_friend, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    final int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private static final int PICK_CONTACT_REQUEST = 1;  // The request code

  private void pickContact() {
    final Intent pickContactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
    startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    // Check which request we're responding to
    if (requestCode == PICK_CONTACT_REQUEST) {
      // Make sure the request was successful
      if (resultCode == RESULT_OK) {
        // The user picked a contact.
        // The Intent's data Uri identifies which contact was selected.

        // Do something with the contact here (bigger example below)
        final Uri contactUri = data.getData();
        Log.d(T, contactUri.toString());


        final String[] projection = {
          ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
          ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
          ContactsContract.CommonDataKinds.Phone.NUMBER,
        };

        final Cursor cursor = getContentResolver()
          .query(contactUri, projection, null, null, null);

        if (cursor.moveToFirst()) {
          final long contactId =
            cursor.getLong(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
          final String lookupKey =
            cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY));

          final Uri lookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
          Log.d(T, lookupUri.toString());
          Log.d(T, contactId + "");
          Log.d(T, lookupKey);

          final String phoneNumber =
            cleanPhoneNumber(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
          Log.d(T, phoneNumber);
          Log.d(T, "Saving friend...");

          final FriendlyDatabaseHelper helper = FriendlyDatabaseHelper.getInstance(this);
          final SQLiteDatabase db = helper.getWritableDatabase();

          final ContentValues contentValues = new ContentValues();
          contentValues.put(FriendContract.FriendEntry.CONTACT_ID, contactId);
          contentValues.put(FriendContract.FriendEntry.LOOKUP_KEY, lookupKey);
          contentValues.put(FriendContract.FriendEntry.NUMBER, phoneNumber);

          try {
            db.insertOrThrow(FriendContract.FriendEntry.TABLE, null, contentValues);
          } catch (final SQLException e) {
            Log.d(T, "Ignoring duplicate friend");
          }
          Log.d(T, "Saved");
        }
      }
    }
  }

  public void backToMainActivity(final View view) {
    finish();
  }

  /**
   * Strips separators and formats the given phone number into a canonical form.
   *
   * @param rawNumber the raw number to clean
   * @return a canonical phone number
   */
  public String cleanPhoneNumber(final String rawNumber) {
    return PhoneNumberUtils.stripSeparators(PhoneNumberUtils.formatNumber(rawNumber));
  }
}
