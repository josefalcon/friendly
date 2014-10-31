package falcon.com.friendly.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import falcon.com.friendly.R;
import falcon.com.friendly.dialog.CallDialog;
import falcon.com.friendly.dialog.FriendDialog;
import falcon.com.friendly.extra.DeleteCursorWrapper;
import falcon.com.friendly.extra.ListViewSwiper;
import falcon.com.friendly.service.CallLogUpdateService;
import falcon.com.friendly.store.Friend;
import falcon.com.friendly.store.FriendlyDatabaseHelper;
import falcon.com.friendly.store.Phone;

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

  private Set<Long> idsToDelete;

  private FriendlyDatabaseHelper databaseHelper;


  public FriendListFragment() {
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // start contact loader
    getLoaderManager().initLoader(0, null, this);
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
    databaseHelper = FriendlyDatabaseHelper.getInstance(getActivity());
  }

  @Override
  public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
    return new CursorLoader(getActivity(), null, null, null, null, null) {
      @Override
      public Cursor loadInBackground() {
        return FriendlyDatabaseHelper.getInstance(getActivity()).listFriends(null);
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

      final FriendlyDatabaseHelper db = FriendlyDatabaseHelper.getInstance(getActivity());
      final Friend friend = db.getFriend(contactId, lookupKey);

      final FriendDialog friendDialog = new FriendDialog();
      final Bundle args = new Bundle(1);
      args.putParcelable("friend", friend);
      friendDialog.setArguments(args);
      friendDialog.show(getFragmentManager(), "FriendDialog");
    }
  }

  /**
   * Removes partially deleted friends and refreshes the cursor.
   */
  private void removeDeletedFriends() {
    if (!idsToDelete.isEmpty()) {
      final FriendlyDatabaseHelper dbHelper = FriendlyDatabaseHelper.getInstance(getActivity());
      for (final Long id : idsToDelete) {
        dbHelper.deleteFriend(id);
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

    final ArrayList<Phone> phoneNumbers = databaseHelper.getPhoneNumbers(holder.id);
    final int count = phoneNumbers.size();
    if (count == 0) {
      Log.e(T, "No phone number found for: " + holder.id);
    } else if (count == 1) {
      final Phone phone = phoneNumbers.get(0);
      Log.d(T, "Dialing friend: " + phone.number);
      final Intent intent = new Intent(Intent.ACTION_DIAL);
      intent.setData(Uri.parse("tel:" + phone.number));
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
    } else {
      final CallDialog callDialog = new CallDialog();
      final Bundle bundle = new Bundle(1);
      bundle.putParcelableArrayList("phoneNumbers", phoneNumbers);
      callDialog.setArguments(bundle);
      callDialog.show(getFragmentManager(), CallDialog.T);
    }
  }
}
