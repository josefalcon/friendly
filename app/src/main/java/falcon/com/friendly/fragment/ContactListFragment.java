package falcon.com.friendly.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import falcon.com.friendly.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ContactListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

  private ListView listView;

  private SimpleCursorAdapter listViewAdapter;

  public ContactListFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // start contact loader
    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.contact_list_fragment, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
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
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Cursor cursor = listViewAdapter.getCursor();
        cursor.moveToPosition(position);

        final long contactId =
          cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        final String lookupKey =
          cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));

        final Uri lookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
        Log.d("ContactListFragment", lookupUri.toString());
        Log.d("ContactListFragment", contactId + "");
        Log.d("ContactListFragment", lookupKey);
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
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(getActivity(),
                            ContactsContract.Contacts.CONTENT_URI,
                            PROJECTION,
                            ContactsContract.Contacts.HAS_PHONE_NUMBER + " = '1'",
                            null, ContactsContract.Contacts.DISPLAY_NAME);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    listViewAdapter.changeCursor(data);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    listViewAdapter.changeCursor(null);
  }
}
