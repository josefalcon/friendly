package falcon.com.friendly.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.widget.AbsListView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;

import java.util.HashSet;
import java.util.Set;

import falcon.com.friendly.R;
import falcon.com.friendly.Util;
import falcon.com.friendly.dialog.FriendDialog;
import falcon.com.friendly.resolver.ContactResolver;
import falcon.com.friendly.service.CallLogUpdateService;
import falcon.com.friendly.service.DeleteFriendService;
import falcon.com.friendly.store.FriendContract;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static falcon.com.friendly.store.FriendContract.FriendEntry;

/**
 * A simple {@link Fragment} subclass for displaying a list of friends.
 */
public class FriendListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

  private static final String T = "FriendFragment";

  private SwipeListView listView;

  private FriendListCursorAdapter listViewAdapter;

  private ContactResolver contactResolver;

  private Set<Long> friendsToDelete;

  public FriendListFragment() {
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // start contact loader
    getLoaderManager().initLoader(0, null, this);

    contactResolver = new ContactResolver(getActivity().getContentResolver());
    friendsToDelete = new HashSet<>();

    if (savedInstanceState != null) {
      final long[] restoreFriendsToDelete = savedInstanceState.getLongArray("friendsToDelete");
      if (restoreFriendsToDelete != null && restoreFriendsToDelete.length > 0) {
        for (final long id : restoreFriendsToDelete) {
          friendsToDelete.add(id);
        }
        clean();
      }
    }
  }

  @Override
  public void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);

    if (isDirty()) {
      final long[] result = new long[friendsToDelete.size()];
      int i = 0;
      for (final Long id : friendsToDelete) {
        result[i++] = id;
      }
      outState.putLongArray("friendsToDelete", result);
    }
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

    listView = (SwipeListView) getActivity().findViewById(R.id.friendListView);
    listViewAdapter = new FriendListCursorAdapter(getActivity(), this);
    listView.setAdapter(listViewAdapter);
    listView.setSwipeListViewListener(new BaseSwipeListViewListener() {
      @Override
      public void onClickFrontView(final int position) {
        showFriendDialog(position);
      }
    });
    listView.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(final AbsListView view, final int scrollState) {
        if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
          clean();
          listView.closeOpenedItems();
        }

        if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_FLING
            && scrollState != SCROLL_STATE_TOUCH_SCROLL) {
          listView.resetScrolling();
        }
      }

      @Override
      public void onScroll(final AbsListView view,
                           final int firstVisibleItem,
                           final int visibleItemCount,
                           final int totalItemCount) {
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

  private boolean isDirty() {
    return !friendsToDelete.isEmpty();
  }

  private void clean() {
    if (isDirty() && deleteFriends(friendsToDelete)) {
      Log.d(T, "Fragment is dirty. Cleaning up...");
      friendsToDelete.clear();
      refresh();
    } else {
      Log.d(T, "Nothing to clean up");
    }
  }

  /**
   * Deletes the given friend ids from the database.
   *
   * @param ids the ids of the friends to delete
   * @return true if this invocation removed a value from the database, false otherwise
   */
  private boolean deleteFriends(final Set<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return false;
    }
    final FriendlyDatabaseHelper databaseHelper = FriendlyDatabaseHelper.getInstance(getActivity());
    final SQLiteDatabase db = databaseHelper.getWritableDatabase();

    final String whereClause = FriendEntry._ID + " IN " + Util.inClausePlaceholders(ids.size());
    final String[] whereArgs = new String[ids.size()];
    int i = 0;
    for (final Long id : ids) {
      whereArgs[i++] = String.valueOf(id);
    }

    return db.delete(FriendEntry.TABLE, whereClause, whereArgs) > 0;
  }

  protected View.OnClickListener makeDeleteFriendOnClickListener(final int position,
                                                                 final View backView,
                                                                 final View deletedView) {
    return new View.OnClickListener() {
      @Override
      public void onClick(final View v) {
        final Cursor cursor = listViewAdapter.getCursor();
        if (cursor.moveToPosition(position)) {
          final long id = cursor.getLong(cursor.getColumnIndex(FriendEntry._ID));
          friendsToDelete.add(id);
          crossfadeViews(backView, deletedView, CROSSFADE_DURATION);
        }
      }
    };
  }

  protected View.OnClickListener makeUndoDeleteOnClickListener(final int position,
                                                               final View backView,
                                                               final View deletedView) {
    return new View.OnClickListener() {
      @Override
      public void onClick(final View v) {
        final Cursor cursor = listViewAdapter.getCursor();
        if (cursor.moveToPosition(position)) {
          final long id = cursor.getLong(cursor.getColumnIndex(FriendEntry._ID));
          friendsToDelete.remove(id);
          listView.closeAnimate(position);
          crossfadeViews(deletedView, backView, CROSSFADE_DURATION);
        }
      }
    };
  }

  private static final int CROSSFADE_DURATION = 400;

  /**
   * Crossfades one view into another with the given duration.
   *
   * @param from     the view to fade out
   * @param to       the view to fade in
   * @param duration the length of the animation
   */
  private void crossfadeViews(final View from, final View to, final long duration) {
    to.setAlpha(0f);
    to.setVisibility(View.VISIBLE);

    to.animate()
      .alpha(1f)
      .setDuration(duration)
      .setListener(null);

    from.animate()
      .alpha(0f)
      .setDuration(duration)
      .setListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(final Animator animation) {
          from.setVisibility(View.GONE);
        }
      });
  }
}
