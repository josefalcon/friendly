package falcon.com.friendly.fragment;


import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.IconButton;
import android.widget.IconTextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;

import falcon.com.friendly.R;
import falcon.com.friendly.TimeUnit;
import falcon.com.friendly.Util;
import falcon.com.friendly.dialog.FriendDialog;
import falcon.com.friendly.resolver.CallLogResolver;
import falcon.com.friendly.resolver.ContactResolver;
import falcon.com.friendly.service.CallLogUpdateService;
import falcon.com.friendly.store.FriendContract;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static falcon.com.friendly.store.FriendContract.*;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

  private static final String T = "FriendFragment";

  private SwipeListView listView;

  private SimpleCursorAdapter listViewAdapter;

  private ContactResolver contactResolver;

  public FriendListFragment() {
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    GREEN = getResources().getColor(R.color.green);
    RED = getResources().getColor(R.color.red);
    ORANGE = getResources().getColor(R.color.orange);

    // start contact loader
    getLoaderManager().initLoader(0, null, this);

    this.contactResolver = new ContactResolver(getActivity().getContentResolver());
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

    listViewAdapter =
      new SimpleCursorAdapter(getActivity(),
                              R.layout.friend_row,
                              null,
                              new String[]{FriendEntry.NUMBER, FriendEntry.LAST_CONTACT},
                              new int[]{R.id.text1, R.id.text2},
                              0) {
        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
          final View view = super.getView(position, convertView, parent);
          final IconTextView frequencyIcon = (IconTextView) view.findViewById(R.id.frequency_icon);
          final IconButton deleteButton = (IconButton) view.findViewById(R.id.delete_button);

          deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
              final Cursor cursor = getCursor();
              if (cursor.moveToPosition(position)) {
                String name =
                  ((TextView) view.findViewById(R.id.text1)).getText().toString();
                if (deleteFriend(cursor.getLong(cursor.getColumnIndex(FriendEntry._ID)))) {
                  listView.closeOpenedItems();
                  if (name == null || name.isEmpty()) {
                    name = "Contact";
                  }
                  Toast.makeText(getActivity(), name + " deleted.", Toast.LENGTH_SHORT).show();
                }
              }
            }
          });

          final Cursor cursor = getCursor();
          if (cursor.moveToPosition(position)) {
            final long lastContact = cursor.getLong(cursor.getColumnIndex(FriendEntry.LAST_CONTACT));
            final long frequency = cursor.getLong(cursor.getColumnIndex(FriendEntry.FREQUENCY));
            final long now = System.currentTimeMillis();
            if (lastContact < 0 || lastContact + frequency <= now) {
              frequencyIcon.setTextColor(RED);
            } else {
              final float scale = (now - lastContact) / (float) frequency;
              frequencyIcon.setTextColor(getColorIndicator(scale));
            }
          }
          return view;
        }
      };

    listViewAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        final TextView textView = (TextView) view;
        if (columnIndex == cursor.getColumnIndex(FriendEntry.LAST_CONTACT)) {
          final long lastContact = cursor.getLong(columnIndex);
          final CharSequence text = Util.getRelativeTime(lastContact, "too long ago!");
          textView.setText(text);
          return true;
        } else if (columnIndex == cursor.getColumnIndex(FriendEntry.NUMBER)) {
          final String name = getContactName(cursor);
          if (name != null && !name.isEmpty()) {
            textView.setText(name);
            return true;
          }
        }
        return false;
      }
    });

    listView.setAdapter(listViewAdapter);
    listView.setSwipeListViewListener(new BaseSwipeListViewListener() {
      @Override
      public void onOpened(final int position, final boolean toRight) {
      }

      @Override
      public void onClosed(final int position, final boolean fromRight) {
      }

      @Override
      public void onListChanged() {
      }

      @Override
      public void onMove(final int position, final float x) {
      }

      @Override
      public void onStartOpen(final int position, final int action, final boolean right) {
        Log.d("swipe", String.format("onStartOpen %d - action %d", position, action));
      }

      @Override
      public void onStartClose(final int position, final boolean right) {
        Log.d("swipe", String.format("onStartClose %d", position));
      }

      @Override
      public void onClickFrontView(final int position) {
        Log.d("swipe", String.format("onClickFrontView %d", position));
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

          Log.d(T, contact.toString());

          final FriendDialog friendDialog = new FriendDialog();
          friendDialog.setArguments(contact);
          friendDialog.show(getFragmentManager(), "FriendDialog");
        }
      }

      @Override
      public void onClickBackView(final int position) {
        Log.d("swipe", String.format("onClickBackView %d", position));
      }

      @Override
      public void onDismiss(final int[] reverseSortedPositions) {
        Log.d("swipe", "onDismiss");
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

  private boolean deleteFriend(final long id) {
    final FriendlyDatabaseHelper databaseHelper =
      FriendlyDatabaseHelper.getInstance(getActivity());
    final SQLiteDatabase db = databaseHelper.getWritableDatabase();
    final int result =
      db.delete(FriendEntry.TABLE, FriendEntry._ID + " = ?", new String[] { String.valueOf(id) });
    if (result > 0) {
      refresh();
      return true;
    }
    return false;
  }
  /**
   * Refreshes the list view associated with this fragment.
   */
  public void refresh() {
    getLoaderManager().restartLoader(0, null, this);
  }

  private String getContactName(final Cursor cursor) {
    final String lookup = cursor.getString(cursor.getColumnIndex(FriendEntry.LOOKUP_KEY));
    final long contactId = cursor.getLong(cursor.getColumnIndex(FriendEntry.CONTACT_ID));

    final Uri lookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookup);
    final Cursor query =
      getActivity().getContentResolver().query(lookupUri,
                                               new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                                               null, null, null);

    try {
      if (query.moveToFirst()) {
        return query.getString(query.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
      }
    } finally {
      query.close();
    }

    return null;
  }

  private static int GREEN;

  private static int ORANGE;

  private static int RED;

  /**
   * Returns a color as an indication of the given scale.
   *
   * @param scale the scale to color
   * @return a color as an indication of the given scale
   */
  private int getColorIndicator(final float scale) {
    int color = RED;
    if (scale < 0.35) {
      color = GREEN;
    } else if (scale < 0.75) {
      color = ORANGE;
    }
    return color;
  }

}
