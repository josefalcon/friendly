package falcon.com.friendly.fragment;


import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
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
import android.widget.IconTextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import falcon.com.friendly.R;
import falcon.com.friendly.resolver.CallLogResolver;
import falcon.com.friendly.service.CallLogUpdateService;
import falcon.com.friendly.store.FriendContract;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static falcon.com.friendly.store.FriendContract.*;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

  private static final String T = "FriendFragment";

  private ListView listView;

  private SimpleCursorAdapter listViewAdapter;

  public FriendFragment() {
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // start contact loader
    getLoaderManager().initLoader(0, null, this);
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

    listViewAdapter =
      new SimpleCursorAdapter(getActivity(),
                              R.layout.friend_entry,
                              null,
                              new String[]{FriendEntry.NUMBER, FriendEntry.LAST_CONTACT},
                              new int[]{R.id.text1, R.id.text2},
                              0) {
        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
          final View view = super.getView(position, convertView, parent);
          final IconTextView frequencyIcon = (IconTextView) view.findViewById(R.id.frequency_icon);
          final Cursor cursor = getCursor();
          if (cursor.moveToPosition(position)) {
            final long lastContact = cursor.getLong(cursor.getColumnIndex(FriendEntry.LAST_CONTACT));
            final long frequency = cursor.getLong(cursor.getColumnIndex(FriendEntry.FREQUENCY));
            final long now = System.currentTimeMillis();
            if (lastContact < 0 || lastContact + frequency <= now) {
              frequencyIcon.setTextColor(RED);
            } else {
              final float scale = (now - lastContact) / (float) frequency;
              Log.d(T, "scale: " + scale);
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

          final CharSequence text;
          if (lastContact < 0) {
            text = "too long ago!";
          } else {
            text = DateUtils.getRelativeTimeSpanString(lastContact,
                                                       System.currentTimeMillis(),
                                                       DateUtils.MINUTE_IN_MILLIS);
          }
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
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        Log.d("FriendFragment", "position: " + position);
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
        return db.query(FriendEntry.TABLE, null, null, null, null, null, null);
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

  private static final int GREEN = Color.rgb(135, 184, 41);

  private static final int ORANGE = Color.rgb(255, 183, 0);

  private static final int RED = Color.rgb(234, 54, 31);

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
