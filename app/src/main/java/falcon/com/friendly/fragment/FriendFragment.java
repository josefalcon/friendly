package falcon.com.friendly.fragment;


import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
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
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import falcon.com.friendly.R;
import falcon.com.friendly.resolver.CallLogResolver;
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
                              android.R.layout.simple_list_item_2,
                              null,
                              new String[]{FriendEntry.NUMBER, FriendEntry.LAST_CONTACT},
                              new int[]{android.R.id.text1, android.R.id.text2},
                              0) {
        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
          final View view = super.getView(position, convertView, parent);

          /*
                  |-----------|-----------|
              last contact    now  lastcontact + frequency
           */
          if (view != null) {
            final Cursor cursor = getCursor();
            if (cursor.moveToPosition(position)) {
              final long lastContact = cursor.getLong(cursor.getColumnIndex(FriendEntry.LAST_CONTACT));
              final long frequency = cursor.getLong(cursor.getColumnIndex(FriendEntry.FREQUENCY));
              final long now = System.currentTimeMillis();
              if (lastContact < 0 || lastContact + frequency <= now) {
                // red
                view.setBackgroundColor(Color.rgb(255, 50, 0));
              } else {
                final float scale = (float) now / (lastContact + frequency);
                final int colorIndex = (int) (scale * 10);

                Log.d(T, "scale: " + scale);
                Log.d(T, "colorIndex: " + colorIndex);

                view.setBackgroundColor(getColorIndicator(colorIndex));
              }
            }
          }
          return view;
        }
      };

    listViewAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        if (columnIndex == cursor.getColumnIndex(FriendEntry.LAST_CONTACT)) {
          final long lastContact = cursor.getLong(columnIndex);
          final TextView textView = (TextView) view;

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

  private int getColorIndicator(final int index) {
    int red = 5;
    int green = 255;
    final int step = 50;

    if (index < 6) {
      red += (index * step);
    } else {
      red = 255;
      green -= ((index - 5) * step);
    }
    return Color.rgb(red, green, 0);
  }
}
