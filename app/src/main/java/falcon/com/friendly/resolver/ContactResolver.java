package falcon.com.friendly.resolver;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;

import falcon.com.friendly.Util;
import falcon.com.friendly.store.FriendContract;

import static android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
import static android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
import static android.provider.ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY;
import static android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER;
import static android.provider.ContactsContract.CommonDataKinds.Phone.TYPE;

public class ContactResolver {

  private final ContentResolver contentResolver;

  public ContactResolver(final ContentResolver contentResolver) {
    this.contentResolver = contentResolver;
  }

  public static final String[] PROJECTION = new String[] {
    CONTACT_ID,
    LOOKUP_KEY,
    NUMBER,
    DISPLAY_NAME,
    TYPE
  };

  /**
   * Returns the contact details for the given contact id and lookup key.
   *
   * @param contactId the contact id of the contact to fetch
   * @param lookupKey the lookup key of the contact to fetch
   * @param type      the phone type of the data to fetch
   * @return a bundle of contact details
   */
  public Bundle getContact(final long contactId,
                           final String lookupKey,
                           final int type) {
    final Cursor cursor =
      contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            PROJECTION,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?" + " AND "
                             + ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY + " = ?" + " AND "
                             + ContactsContract.CommonDataKinds.Phone.TYPE + " = ?",
                            new String[]{String.valueOf(contactId), lookupKey, String.valueOf(type)}, null);
    return getContact(cursor);
  }

  /**
   * Returns the contact details for the given contact URI.
   *
   * @param contactUri the URI of the contact to fetch
   * @return a bundle of contact details
   */
  public Bundle getContact(final Uri contactUri) {
    final Cursor cursor = contentResolver.query(contactUri, PROJECTION, null, null, null);
    return getContact(cursor);
  }

  private Bundle getContact(final Cursor cursor) {
    try {
      if (cursor.moveToFirst()) {
        final long contactId = cursor.getLong(cursor.getColumnIndex(CONTACT_ID));
        final String lookupKey = cursor.getString(cursor.getColumnIndex(LOOKUP_KEY));
        final String rawNumber = cursor.getString(cursor.getColumnIndex(NUMBER));
        final String displayName = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
        final int phoneType = cursor.getInt(cursor.getColumnIndex(TYPE));

        final Bundle bundle = new Bundle();
        bundle.putLong(CONTACT_ID, contactId);
        bundle.putString(LOOKUP_KEY, lookupKey);
        bundle.putString(NUMBER, Util.cleanPhoneNumber(rawNumber));
        bundle.putString(DISPLAY_NAME, displayName);
        bundle.putInt(TYPE, phoneType);
        return bundle;
      }
    } finally {
      cursor.close();
    }
    return null;
  }

  /**
   * Converts the given bundle of contact details to content values, suitable for storing in a
   * database.
   *
   * @param contact the bundle of details to convert
   * @return a new ContentValues object
   */
  public ContentValues getFriendContentValues(final Bundle contact) {
    final ContentValues contentValues = new ContentValues();
    contentValues.put(FriendContract.FriendEntry.CONTACT_ID, contact.getLong(CONTACT_ID));
    contentValues.put(FriendContract.FriendEntry.LOOKUP_KEY, contact.getString(LOOKUP_KEY));
    contentValues.put(FriendContract.FriendEntry.NUMBER, contact.getString(NUMBER));
    contentValues.put(FriendContract.FriendEntry.TYPE, contact.getInt(TYPE));
    contentValues.put(FriendContract.FriendEntry.LAST_CONTACT,
                      contact.getLong(FriendContract.FriendEntry.LAST_CONTACT));
    contentValues.put(FriendContract.FriendEntry.FREQUENCY,
                      contact.getLong(FriendContract.FriendEntry.FREQUENCY));

    return contentValues;
  }

}
