package falcon.com.friendly.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import falcon.com.friendly.R;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static falcon.com.friendly.store.FriendContract.FriendEntry;

/**
 * A simple {@link Fragment} subclass.
 */
public class ContactListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

  public static final String TAG = "ContactListFragment";

  private ListView listView;

  private SimpleCursorAdapter listViewAdapter;

  public ContactListFragment() {
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // start contact loader
    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public View onCreateView(final LayoutInflater inflater,
                           final ViewGroup container,
                           final Bundle savedInstanceState) {
    return inflater.inflate(R.layout.contact_list_fragment, container, false);
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    listView = (ListView) getActivity().findViewById(R.id.contactListView);

    listViewAdapter =
      new SimpleCursorAdapter(getActivity(),
                              android.R.layout.simple_list_item_1,
                              null,
                              new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                              new int[]{android.R.id.text1},
                              0);

    listView.setAdapter(listViewAdapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        final Cursor cursor = listViewAdapter.getCursor();
        cursor.moveToPosition(position);

        final long contactId =
          cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        final String lookupKey =
          cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));

        final Uri lookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
        Log.d(TAG, lookupUri.toString());
        Log.d(TAG, contactId + "");
        Log.d(TAG, lookupKey);

        final String phoneNumber = getPhoneNumber(contactId);
        Log.d(TAG, "Phone number: " + phoneNumber);
        Log.d(TAG, "Saving friend...");

        final FriendlyDatabaseHelper helper = FriendlyDatabaseHelper.getInstance(getActivity());
        final SQLiteDatabase db = helper.getWritableDatabase();

        final ContentValues contentValues = new ContentValues();
        contentValues.put(FriendEntry.CONTACT_ID, contactId);
        contentValues.put(FriendEntry.LOOKUP_KEY, lookupKey);
        contentValues.put(FriendEntry.NUMBER, phoneNumber);

        try {
          db.insertOrThrow(FriendEntry.TABLE, null, contentValues);
        } catch (final SQLException e) {
          Log.d(TAG, "Ignoring duplicate friend");
        }
        Log.d(TAG, "Saved");
      }
    });
  }

  private static final String[] PROJECTION = new String[] {
    ContactsContract.Contacts._ID,
    ContactsContract.Contacts.LOOKUP_KEY,
    ContactsContract.Contacts.DISPLAY_NAME,
    ContactsContract.Contacts.HAS_PHONE_NUMBER,
  };

  @Override
  public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
    return new CursorLoader(getActivity(),
                            ContactsContract.Contacts.CONTENT_URI,
                            PROJECTION,
                            ContactsContract.Contacts.HAS_PHONE_NUMBER + " = '1'",
                            null, ContactsContract.Contacts.DISPLAY_NAME);
  }

  @Override
  public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
    listViewAdapter.changeCursor(data);
  }

  @Override
  public void onLoaderReset(final Loader<Cursor> loader) {
    listViewAdapter.changeCursor(null);
  }

  private String getPhoneNumber(final long id) {
    final Cursor cursor = getActivity().getContentResolver().query(
      ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
      null,
      ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
      new String[] { String.valueOf(id) },
      null);

    try {
      if (cursor.moveToFirst()) {
        final String number =
          cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        return PhoneNumberUtils.stripSeparators(PhoneNumberUtils.formatNumber(number));
      }
    } finally {
      cursor.close();
    }
    return "";
  }
}
