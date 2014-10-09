package falcon.com.friendly.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import falcon.com.friendly.R;
import falcon.com.friendly.dialog.FriendDialog;
import falcon.com.friendly.resolver.ContactResolver;
import falcon.com.friendly.service.CallLogUpdateService;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static falcon.com.friendly.store.FriendContract.FriendEntry;

/**
 * A simple {@link Fragment} subclass for displaying a list of friends.
 */
public class FriendListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

  private static final String T = "FriendFragment";

  private ListView listView;

  private FriendListCursorAdapter listViewAdapter;

  private ContactResolver contactResolver;

  public FriendListFragment() {
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // start contact loader
    getLoaderManager().initLoader(0, null, this);

    contactResolver = new ContactResolver(getActivity().getContentResolver());
  }

  @Override
  public void onStart() {
    super.onStart();

    final Intent intent = new Intent(getActivity(), CallLogUpdateService.class);
    getActivity().startService(intent);
  }

  @Override
  public View onCreateView(final LayoutInflater inflater,
                           final ViewGroup container,
                           final Bundle savedInstanceState) {
    return inflater.inflate(R.layout.friend_fragment, container, false);
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    listView = (ListView) getActivity().findViewById(R.id.friend_list_view);
    listViewAdapter = new FriendListCursorAdapter(getActivity(), this);
    listView.setAdapter(listViewAdapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(final AdapterView<?> parent,
                              final View view,
                              final int position,
                              final long id) {
        showFriendDialog(position);
      }
    });
  }

  @Override
  public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
    return new CursorLoader(getActivity(), null, null, null, null, null) {
      @Override
      public Cursor loadInBackground() {
        final SQLiteDatabase db =
          FriendlyDatabaseHelper.getInstance(getActivity()).getReadableDatabase();
        final String query =
          "SELECT *, max(last_contact) FROM friend GROUP BY contact_id, lookup_key "
          + "ORDER BY (strftime('%s','now') - (last_contact / 1000)) / (1.0 * frequency) DESC";
        return db.rawQuery(query, null);
      }
    };
  }

  @Override
  public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
    listViewAdapter.changeCursor(data);
  }

  @Override
  public void onLoaderReset(final Loader<Cursor> loader) {
    listViewAdapter.changeCursor(null);
  }

  /**
   * Refreshes the list view associated with this fragment.
   */
  public void refresh() {
    getLoaderManager().restartLoader(0, null, this);
  }

  /**
   * Shows a FriendDialog for the friend at the given position.
   *
   * @param position the position of the friend to show
   */
  private void showFriendDialog(final int position) {
    Log.d(T, "Showing friend at position " + position);
    final Cursor cursor = listViewAdapter.getCursor();
    if (cursor.moveToPosition(position)) {
      final long contactId = cursor.getLong(cursor.getColumnIndex(FriendEntry.CONTACT_ID));
      final String lookupKey = cursor.getString(cursor.getColumnIndex(FriendEntry.LOOKUP_KEY));
      final int type = cursor.getInt(cursor.getColumnIndex(FriendEntry.TYPE));
      final long lastContact = cursor.getLong(cursor.getColumnIndex(FriendEntry.LAST_CONTACT));
      final long frequency = cursor.getLong(cursor.getColumnIndex(FriendEntry.FREQUENCY));

      final Bundle contact = contactResolver.getContact(contactId, lookupKey, type);
      contact.putLong(FriendEntry.CONTACT_ID, contactId);
      contact.putString(FriendEntry.LOOKUP_KEY, lookupKey);
      contact.putInt(FriendEntry.TYPE, type);
      contact.putLong(FriendEntry.LAST_CONTACT, lastContact);
      contact.putLong(FriendEntry.FREQUENCY, frequency);

      final FriendDialog friendDialog = new FriendDialog();
      friendDialog.setArguments(contact);
      friendDialog.show(getFragmentManager(), "FriendDialog");
    }
  }

}
