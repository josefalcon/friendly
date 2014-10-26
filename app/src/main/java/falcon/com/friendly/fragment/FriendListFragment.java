package falcon.com.friendly.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.cocosw.undobar.UndoBarController;
import com.cocosw.undobar.UndoBarStyle;

import java.util.HashSet;
import java.util.Set;

import falcon.com.friendly.R;
import falcon.com.friendly.dialog.FriendDialog;
import falcon.com.friendly.extra.DeleteCursorWrapper;
import falcon.com.friendly.extra.ListViewSwiper;
import falcon.com.friendly.resolver.ContactResolver;
import falcon.com.friendly.service.CallLogUpdateService;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static com.cocosw.undobar.UndoBarController.UndoBar;
import static falcon.com.friendly.store.FriendContract.FriendEntry;

/**
 * A simple {@link Fragment} subclass for displaying a list of friends.
 */
public class FriendListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
                                                            ListViewSwiper.SwipeListener {

  private static final String T = "FriendFragment";

  private static final UndoBarStyle UNDO_BAR_STYLE =
    new UndoBarStyle(-1, com.cocosw.undobar.R.string.undo);

  private ListView listView;

  private ListViewSwiper listViewSwiper;

  private FriendListCursorAdapter listAdapter;

  private ContactResolver contactResolver;

  private Set<Long> idsToDelete;

  public FriendListFragment() {
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // start contact loader
    getLoaderManager().initLoader(0, null, this);

    contactResolver = new ContactResolver(getActivity().getContentResolver());
    idsToDelete = new HashSet<>();
  }

  @Override
  public void onStop() {
    super.onStop();
    removeDeletedFriends();
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
    final View view = inflater.inflate(R.layout.friend_fragment, container, false);
    listView = (ListView) view.findViewById(R.id.friend_list_view);
    listView.setEmptyView(view.findViewById(R.id.empty_list_view));
    return view;
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    listViewSwiper = new ListViewSwiper(listView, this);
    listAdapter = new FriendListCursorAdapter(getActivity(),
                                              listViewSwiper.getOnTouchListener());
    listView.setAdapter(listAdapter);
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
          "SELECT *, max(last_contact), min(frequency) FROM friend GROUP BY contact_id, lookup_key "
          + "ORDER BY (CAST(strftime('%s','now') as integer) - (last_contact / 1000)) / (1.0 * frequency) DESC";
        return db.rawQuery(query, null);
      }
    };
  }

  @Override
  public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
    listAdapter.changeCursor(data);
  }

  @Override
  public void onLoaderReset(final Loader<Cursor> loader) {
    listAdapter.changeCursor(null);
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
    final Cursor cursor = listAdapter.getCursor();
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

  /**
   * Deletes the given friend id from the database.
   *
   * @param id the id of the friend to delete
   * @return true if this invocation removed a value from the database, false otherwise
   */
  private boolean deleteFriend(final long id) {
    final FriendlyDatabaseHelper databaseHelper = FriendlyDatabaseHelper.getInstance(getActivity());
    final SQLiteDatabase db = databaseHelper.getWritableDatabase();

    final String whereClause = FriendEntry._ID + " = ?";
    final String[] whereArgs = new String[] { String.valueOf(id) };
    return db.delete(FriendEntry.TABLE, whereClause, whereArgs) > 0;
  }

  private void removeDeletedFriends() {
    if (!idsToDelete.isEmpty()) {
      for (final Long id : idsToDelete) {
        deleteFriend(id);
      }
      idsToDelete.clear();
      refresh();
    }
  }

  @Override
  public void onMoveLeft(final View view) {
    final FrameLayout parent = (FrameLayout) view.getParent();
    final FriendListCursorAdapter.ViewHolder holder =
      (FriendListCursorAdapter.ViewHolder) parent.getTag();
    holder.callBackdropView.setVisibility(View.GONE);
    holder.deleteBackdropView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onSwipedLeft(final View view) {
    final FrameLayout parent = (FrameLayout) view.getParent();
    final FriendListCursorAdapter.ViewHolder holder =
      (FriendListCursorAdapter.ViewHolder) parent.getTag();

    // fake the delete
    final long id = holder.id;
    idsToDelete.add(id);

    final int position = listView.getPositionForView(view);
    final Cursor originalCursor = listAdapter.getCursor();
    final DeleteCursorWrapper cursorWrapper =
      new DeleteCursorWrapper(originalCursor, position);

    listAdapter.swapCursor(cursorWrapper);

    final UndoBar undoBar = new UndoBar(getActivity()).style(UNDO_BAR_STYLE);
    undoBar.message("Removed " + holder.contactName);
    undoBar.listener(new UndoBarController.AdvancedUndoListener() {
      @Override
      public void onHide(final Parcelable parcelable) {
        onClear();
      }

      @Override
      public void onClear() {
        removeDeletedFriends();
      }

      @Override
      public void onUndo(final Parcelable parcelable) {
        listAdapter.swapCursor(originalCursor);
        idsToDelete.remove(id);
      }
    });
    undoBar.show();
  }

  @Override
  public void onMoveRight(final View view) {
    final FrameLayout parent = (FrameLayout) view.getParent();
    final FriendListCursorAdapter.ViewHolder holder =
      (FriendListCursorAdapter.ViewHolder) parent.getTag();
    holder.callBackdropView.setVisibility(View.VISIBLE);
    holder.deleteBackdropView.setVisibility(View.GONE);
  }

  @Override
  public void onSwipedRight(final View view) {
    final FrameLayout parent = (FrameLayout) view.getParent();
    final FriendListCursorAdapter.ViewHolder holder =
      (FriendListCursorAdapter.ViewHolder) parent.getTag();

    Log.d(T, "Dialing friend: " + holder.number);
    final Intent intent = new Intent(Intent.ACTION_DIAL);
    intent.setData(Uri.parse("tel:" + holder.number));
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
  }
}
