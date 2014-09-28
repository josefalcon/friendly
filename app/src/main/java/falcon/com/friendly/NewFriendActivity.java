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
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import falcon.com.friendly.resolver.CallLogResolver;
import falcon.com.friendly.store.FriendContract;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static android.provider.ContactsContract.CommonDataKinds.Phone.*;


public class NewFriendActivity extends Activity {

  private static final String T = "NewFriendActivity";

  private static final int PICK_CONTACT_REQUEST = 1;

  private static enum TimeUnit {
    MINUTES(60000L),
    HOURS(3600000L),
    DAYS(86400000L),
    WEEKS(604800000L),
    MONTHS(2629740000L);

    private static final String[] TIME_UNIT_STRINGS = new String[values().length];
    static {
      final TimeUnit[] values = values();
      for (int i = 0; i < TIME_UNIT_STRINGS.length; i++) {
        TIME_UNIT_STRINGS[i] = values[i].name();
      }
    }

    final long milliseconds;

    TimeUnit(final long milliseconds) {
      this.milliseconds = milliseconds;
    }

    long inMilliseconds(final int multiplier) {
      return multiplier * milliseconds;
    }

    static String[] stringValues() {
      return TIME_UNIT_STRINGS;
    }

    static TimeUnit getByIndex(final int index) {
      if (index >= 0 && index < TIME_UNIT_STRINGS.length) {
        return values()[index];
      }
      return null;
    }
  }

  private boolean contactPicked;

  private ContentValues contentValues;

  private CallLogResolver callLogResolver;

  private NumberPicker quantityPicker;

  private NumberPicker timeUnitPicker;

  private TextView contactName;

  private TextView contactPhone;

  private TextView contactPhoneType;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_new_friend);
    getActionBar().setDisplayHomeAsUpEnabled(true);

    callLogResolver = new CallLogResolver(getContentResolver());
    quantityPicker = (NumberPicker) findViewById(R.id.quantity);
    timeUnitPicker = (NumberPicker) findViewById(R.id.time_unit);
    contactName = (TextView) findViewById(R.id.contact_name);
    contactPhone = (TextView) findViewById(R.id.contact_phone);
    contactPhoneType = (TextView) findViewById(R.id.contact_phone_type);

    quantityPicker.setMinValue(1);
    quantityPicker.setMaxValue(60);

    final String[] timeUnitStrings = TimeUnit.stringValues();
    timeUnitPicker.setMinValue(0);
    timeUnitPicker.setMaxValue(timeUnitStrings.length - 1);
    timeUnitPicker.setDisplayedValues(timeUnitStrings);
    timeUnitPicker.setWrapSelectorWheel(false);

    if (savedInstanceState != null) {
      contactPicked = savedInstanceState.getBoolean("contactPicked");
      contactName.setText(savedInstanceState.getString("contactName"));
      contactPhone.setText(savedInstanceState.getString("contactPhone"));
      contactPhoneType.setText(savedInstanceState.getString("contactPhoneType"));
      quantityPicker.setValue(savedInstanceState.getInt("quantityPicker"));
      timeUnitPicker.setValue(savedInstanceState.getInt("timeUnitPicker"));
      contentValues = savedInstanceState.getParcelable("contentValues");
    }

    if (!contactPicked) {
      final Intent pickContactIntent =
        new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
      pickContactIntent.setType(CONTENT_TYPE);
      startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }
  }

  @Override
  protected void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("contactPicked", contactPicked);
    if (contactPicked) {
      outState.putString("contactName", contactName.getText().toString());
      outState.putString("contactPhone", contactPhone.getText().toString());
      outState.putString("contactPhoneType", contactPhoneType.getText().toString());
      outState.putInt("quantityPicker", quantityPicker.getValue());
      outState.putInt("timeUnitPicker", timeUnitPicker.getValue());
      outState.putParcelable("contentValues", contentValues);
    }
  }

  public static final String[] PROJECTION = new String[] {
    CONTACT_ID,
    LOOKUP_KEY,
    NUMBER,
    DISPLAY_NAME,
    TYPE
  };

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
      contactPicked = true;
      final Uri contactUri = data.getData();
      final Cursor cursor = getContentResolver().query(contactUri, PROJECTION, null, null, null);
      try {
        if (cursor.moveToFirst()) {
          final long contactId = cursor.getLong(cursor.getColumnIndex(CONTACT_ID));
          final String lookupKey = cursor.getString(cursor.getColumnIndex(LOOKUP_KEY));
          final String rawNumber = cursor.getString(cursor.getColumnIndex(NUMBER));
          final String displayName = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));

          final int phoneType = cursor.getInt(cursor.getColumnIndex(TYPE));
          final String phoneTypeString =
            getResources()
              .getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(phoneType));

          final String cleanPhoneNumber = cleanPhoneNumber(rawNumber);
          final long lastContact = callLogResolver.getLastContact(cleanPhoneNumber);

          contentValues = new ContentValues();
          contentValues.put(FriendContract.FriendEntry.CONTACT_ID, contactId);
          contentValues.put(FriendContract.FriendEntry.LOOKUP_KEY, lookupKey);
          contentValues.put(FriendContract.FriendEntry.NUMBER, cleanPhoneNumber);
          contentValues.put(FriendContract.FriendEntry.LAST_CONTACT, lastContact);

          contactName.setText(displayName);
          contactPhone.setText(PhoneNumberUtils.formatNumber(rawNumber));
          contactPhoneType.setText(phoneTypeString + " ");
        }
      } finally {
        cursor.close();
      }
    } else {
      finish();
    }
  }

  /**
   * Saves the selected contact into the database and returns back to the parent activity.
   *
   * @param view the calling view
   */
  public void saveAndFinish(final View view) {
    int result = RESULT_CANCELED;
    Intent intent = null;

    final int quantity = quantityPicker.getValue();
    final TimeUnit timeUnit = TimeUnit.getByIndex(timeUnitPicker.getValue());
    final long frequency = timeUnit.inMilliseconds(quantity);
    if (contentValues != null && frequency > 0) {
      contentValues.put(FriendContract.FriendEntry.FREQUENCY, frequency);

      Log.d(T, "Values to be saved...");
      for (final Map.Entry<String, Object> entry : contentValues.valueSet()) {
        Log.d(T, entry.getKey() + ", " + entry.getValue().toString());
      }

      if (saveFriend(contentValues)) {
        result = RESULT_OK;
        intent = new Intent();
        intent.putExtra("name", contactName.getText());
      }
    }

    setResult(result, intent);
    finish();
  }

  /**
   * Persists the given ContentValues to the Friend database table.
   *
   * @param contentValues the ContentValues representing a Friend.
   */
  private boolean saveFriend(final ContentValues contentValues) {
    final FriendlyDatabaseHelper helper = FriendlyDatabaseHelper.getInstance(this);
    final SQLiteDatabase db = helper.getWritableDatabase();
    try {
      final long id = db.insertOrThrow(FriendContract.FriendEntry.TABLE, null, contentValues);
      return id != -1;
    } catch (final SQLException e) {
      Log.d(T, "Ignoring duplicate friend...");
    }
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
