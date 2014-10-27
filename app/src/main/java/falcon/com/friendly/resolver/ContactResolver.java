package falcon.com.friendly.resolver;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;

import falcon.com.friendly.Util;
import falcon.com.friendly.store.Friend;
import falcon.com.friendly.store.Phone;

import static android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
import static android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
import static android.provider.ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY;
import static android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER;
import static android.provider.ContactsContract.CommonDataKinds.Phone.TYPE;

public class ContactResolver {

  private final ContentResolver contentResolver;

  private final CallLogResolver callLogResolver;

  public ContactResolver(final ContentResolver contentResolver) {
    this.contentResolver = contentResolver;
    this.callLogResolver = new CallLogResolver(contentResolver);
  }

  public static final String[] PROJECTION = new String[] {
    CONTACT_ID,
    LOOKUP_KEY,
    NUMBER,
    DISPLAY_NAME,
    TYPE
  };

  /**
   * Returns the contact details for the given contact URI as a Friend object.
   *
   * @param contactUri the URI of the contact to fetch
   * @return the contact details or null
   */
  public Friend getContactAsFriend(final Uri contactUri) {
    final Cursor cursor = contentResolver.query(contactUri, PROJECTION, null, null, null);
    try {
      if (cursor.moveToFirst()) {
        final Friend friend = new Friend();
        friend.contactId = cursor.getLong(cursor.getColumnIndex(CONTACT_ID));
        friend.lookupKey = cursor.getString(cursor.getColumnIndex(LOOKUP_KEY));
        friend.displayName = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));

        final Phone phone = new Phone();
        phone.number = Util.cleanPhoneNumber(cursor.getString(cursor.getColumnIndex(NUMBER)));
        phone.type = cursor.getInt(cursor.getColumnIndex(TYPE));

        friend.lastContact = callLogResolver.getLastContact(phone.number);
        friend.numbers = new ArrayList<>(1);
        friend.numbers.add(phone);
        return friend;
      }
    } finally {
      cursor.close();
    }
    return null;
  }

}
