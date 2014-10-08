package falcon.com.friendly.fragment;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.IconTextView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import falcon.com.friendly.R;
import falcon.com.friendly.Util;

import static falcon.com.friendly.store.FriendContract.FriendEntry;

/**
 * A CursorAdapter for the FriendListFragment.
 */
public class FriendListCursorAdapter extends ResourceCursorAdapter {

  private final Context context;

  private final FriendListFragment friendListFragment;

  public FriendListCursorAdapter(final Context context,
                                 final FriendListFragment friendListFragment) {
    super(context, R.layout.friend_row, null, 0);
    this.context = context;
    this.friendListFragment = friendListFragment;
  }

  @Override
  public View getView(final int position, final View convertView, final ViewGroup parent) {
    final View view = super.getView(position, convertView, parent);
    final View backView = view.findViewById(R.id.back);
    final View deleteButton = view.findViewById(R.id.delete_button);
    final View deletedView = view.findViewById(R.id.deleted);
    final View undoButton = view.findViewById(R.id.undo_button);

    deleteButton.setOnClickListener(
      friendListFragment.makeDeleteFriendOnClickListener(position, backView, deletedView));

    undoButton.setOnClickListener(
      friendListFragment.makeUndoDeleteOnClickListener(position, backView, deletedView));

    return view;
  }

  @Override
  public void bindView(final View view, final Context context, final Cursor cursor) {
    final TextView contactNameView = (TextView) view.findViewById(R.id.contact_name);
    final TextView lastContactView = (TextView) view.findViewById(R.id.last_contact);
    final IconTextView frequencyIconView = (IconTextView) view.findViewById(R.id.frequency_icon);

    final String name = getContactName(cursor);
    contactNameView.setText(name);

    final long lastContact = cursor.getLong(cursor.getColumnIndex(FriendEntry.LAST_CONTACT));
    final CharSequence text = Util.getRelativeTime(lastContact, "too long ago!");
    lastContactView.setText(text);

    final long frequency = cursor.getLong(cursor.getColumnIndex(FriendEntry.FREQUENCY));
    final long now = System.currentTimeMillis();
    if (lastContact < 0 || lastContact + frequency <= now) {
      frequencyIconView.setTextColor(RED);
    } else {
      final float scale = (now - lastContact) / (float) frequency;
      frequencyIconView.setTextColor(getColorIndicator(scale));
    }
  }

  private static final String[] DISPLAY_NAME_PROJECTION =
    new String[]{ContactsContract.Contacts.DISPLAY_NAME};

  /**
   * Returns the contact's display name for the given cursor.
   *
   * @param cursor the cursor to fetch a name for
   * @return the contact's display name
   */
  private String getContactName(final Cursor cursor) {
    final String lookup =
      cursor.getString(cursor.getColumnIndex(FriendEntry.LOOKUP_KEY));
    final long contactId =
      cursor.getLong(cursor.getColumnIndex(FriendEntry.CONTACT_ID));

    final Uri lookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookup);
    final Cursor query =
      context.getContentResolver().query(lookupUri, DISPLAY_NAME_PROJECTION, null, null, null);
    try {
      if (query.moveToFirst()) {
        return query.getString(query.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
      }
    } finally {
      query.close();
    }
    return "Unknown contact";
  }

  private static final int GREEN = Color.parseColor("#ff87B829");

  private static final int ORANGE = Color.parseColor("#ffFFB700");

  private static final int RED = Color.parseColor("#ffEA361F");

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
