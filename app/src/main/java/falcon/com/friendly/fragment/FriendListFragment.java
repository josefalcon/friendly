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
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

  private Map<Integer, Long> friendsToDelete;

  private Set<Integer> swipedPositions;

  public FriendListFragment() {
    swipedPositions = new HashSet<>();
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // start contact loader
    getLoaderManager().initLoader(0, null, this);

    contactResolver = new ContactResolver(getActivity().getContentResolver());
    friendsToDelete = new HashMap<>();

//    if (savedInstanceState != null) {
//      final long[] restoreFriendsToDelete = savedInstanceState.getLongArray("friendsToDelete");
//      if (restoreFriendsToDelete != null && restoreFriendsToDelete.length > 0) {
//        for (final long id : restoreFriendsToDelete) {
//          friendsToDelete.add(id);
//        }
//        clean();
//      }
//    }
  }

  @Override
  public void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);

//    if (isDirty()) {
//      final long[] result = new long[friendsToDelete.size()];
//      int i = 0;
//      for (final Map.Entry<Integer, Long> entry : friendsToDelete.entrySet()) {
//        result[i++] = id;
//      }
//      outState.putLongArray("friendsToDelete", result);
//    }
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

    listView = (ListView) getActivity().findViewById(R.id.friendListView);
    listViewAdapter = new FriendListCursorAdapter(getActivity(), this, mTouchListener);
    listView.setAdapter(listViewAdapter);
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

  private View getViewAt(int position) {
    int firstPosition = listView.getFirstVisiblePosition() - listView.getHeaderViewsCount();
    int wantedChild = position - firstPosition;
    if (wantedChild < 0 || wantedChild >= listView.getChildCount()) {
      return null;
    }
    return listView.getChildAt(wantedChild);
  }

  private void clean() {
    Log.d(T, "Fragment is dirty. Cleaning up...");
    for (final Map.Entry<Integer, Long> entry : friendsToDelete.entrySet()) {
      Log.d(T, "Dismissing: " + entry.getKey());
      animateRemoval(getViewAt(entry.getKey()));
    }
    friendsToDelete.clear();
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


  protected View.OnClickListener makeUndoDeleteOnClickListener(final int position,
                                                               final View frontView,
                                                               final View backView,
                                                               final View deletedView) {
    return new View.OnClickListener() {
      @Override
      public void onClick(final View v) {
        friendsToDelete.remove(position);
        closeView(frontView, new Runnable() {
          @Override
          public void run() {
            deletedView.setVisibility(View.GONE);
            deletedView.setAlpha(0f);
            backView.setVisibility(View.VISIBLE);
            backView.setAlpha(1f);
          }
        });
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

  private void closeView(final View v, final Runnable callback) {
    v.setTranslationX(-v.getWidth());
    v.setVisibility(View.VISIBLE);
    final long duration = 250;
    listView.setEnabled(false);
    v.animate().setDuration(duration).
      translationX(0).
      withEndAction(new Runnable() {
        @Override
        public void run() {
          listView.setEnabled(true);
          callback.run();
        }
      });
  }

  boolean mSwiping = false;
  boolean mItemPressed = false;

  private static final int LEFT = -1;
  private static final int RIGHT = 1;

  private View.OnTouchListener mTouchListener = new View.OnTouchListener() {

    float mDownX;
    private int mSwipeSlop = 12;
    int direction = 0;

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
      final ViewGroup parent = (ViewGroup) v.getParent();
      final View backView = parent.findViewById(R.id.back);
      final View deletedView = parent.findViewById(R.id.deleted);
      final View callView = parent.findViewById(R.id.call);
      final int viewPosition = listView.getPositionForView(v);

      if (mSwipeSlop < 0) {
        mSwipeSlop = ViewConfiguration.get(getActivity()).getScaledTouchSlop();
      }
      switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          if (mItemPressed) {
            return false;
          }
          mItemPressed = true;
          mDownX = event.getX();
          break;
        case MotionEvent.ACTION_CANCEL:
          v.setTranslationX(0);
          mItemPressed = false;
          break;
        case MotionEvent.ACTION_MOVE:
        {
          final float x = event.getX() + v.getTranslationX();
          final float deltaX = x - mDownX;
          if (deltaX < 0) {
            direction = LEFT;
            callView.setVisibility(View.GONE);
          } else {
            direction = RIGHT;
            callView.setVisibility(View.VISIBLE);
          }

          final float deltaXAbs = Math.abs(deltaX);
          if (!mSwiping) {
            if (deltaXAbs > mSwipeSlop) {
              mSwiping = true;
              listView.requestDisallowInterceptTouchEvent(true);
            }
          }

          if (mSwiping) {
            v.setTranslationX(deltaX);
          }
        }
        break;
        case MotionEvent.ACTION_UP:
        {
          // User let go - figure out whether to animate the view out, or back into place
          if (mSwiping) {
            final float x = event.getX() + v.getTranslationX();
            final float deltaX = x - mDownX;
            final float deltaXAbs = Math.abs(deltaX);
            final float fractionCovered;
            final float endX;
            final boolean remove;

            // TODO: for now we don't let right swipes succeed
            if (direction != RIGHT && deltaXAbs > v.getWidth() * 0.75) {
              // Greater than a quarter of the width - animate it out
              fractionCovered = deltaXAbs / v.getWidth();
              endX = deltaX < 0 ? -v.getWidth() : v.getWidth();
              remove = true;
            } else {
              // Not far enough - animate it back
              fractionCovered = 1 - (deltaXAbs / v.getWidth());
              endX = 0;
              remove = false;
            }

            final long duration = (int) ((1 - fractionCovered) * 250);
            listView.setEnabled(false);
            v.animate().setDuration(duration).
              translationX(endX).
              withEndAction(new Runnable() {
                @Override
                public void run() {
                  // Restore animated values
                  if (remove) {
                    if (direction == RIGHT) {
                      Log.d(T, "call action!");
                    } else if (direction == LEFT) {
//                      final long id = listViewAdapter.getItemId(viewPosition);
//                      friendsToDelete.put(viewPosition, id);
//                      crossfadeViews(backView, deletedView, CROSSFADE_DURATION);
                      animateRemoval(v);
                    }
                    swipedPositions.add(viewPosition);
                  }

                  mSwiping = false;
                  listView.setEnabled(true);
                }
              });
          } else {
            if (!swipedPositions.contains(viewPosition)) {
              v.playSoundEffect(SoundEffectConstants.CLICK);
              showFriendDialog(viewPosition);
            }
          }
        }
        mItemPressed = false;
        break;
        default:
          return false;
      }
      return true;
    }
  };

  HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();

  private void animateRemoval(final View viewToRemove) {
    final int firstVisiblePosition = listView.getFirstVisiblePosition();
    for (int i = 0; i < listView.getChildCount(); ++i) {
      final View child = listView.getChildAt(i);
      if (child != viewToRemove) {
        final int position = firstVisiblePosition + i;
        final long itemId = listViewAdapter.getItemId(position);
        mItemIdTopMap.put(itemId, child.getTop());
      }
    }
    // Delete the item from the adapter
    final int position = listView.getPositionForView(viewToRemove);
    deleteFriend(listViewAdapter.getItemId(position));
    refresh();

    final ViewTreeObserver observer = listView.getViewTreeObserver();
    observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
      public boolean onPreDraw() {
        observer.removeOnPreDrawListener(this);
        boolean firstAnimation = true;
        final int firstVisiblePosition = listView.getFirstVisiblePosition();
        for (int i = 0; i < listView.getChildCount(); ++i) {
          final View child = listView.getChildAt(i);
          final int position = firstVisiblePosition + i;
          final long itemId = listViewAdapter.getItemId(position);
          final Integer startTop = mItemIdTopMap.get(itemId);
          final int top = child.getTop();
          if (startTop != null) {
            if (startTop != top) {
              final int delta = startTop - top;
              child.setTranslationY(delta);
              child.animate().setDuration(550).translationY(0);
              if (firstAnimation) {
                child.animate().withEndAction(new Runnable() {
                  public void run() {
                    Log.d(T, "done");
                    mSwiping = false;
                    listView.setEnabled(true);
                  }
                });
                firstAnimation = false;
              }
            }
          }
        }
        mItemIdTopMap.clear();
        return true;
      }
    });
  }
}
