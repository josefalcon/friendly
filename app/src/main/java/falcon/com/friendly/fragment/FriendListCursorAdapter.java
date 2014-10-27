package falcon.com.friendly.fragment;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.IconTextView;
import android.widget.RelativeLayout;
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

  private final View.OnTouchListener touchListener;

  public FriendListCursorAdapter(final Context context,
                                 final View.OnTouchListener touchListener) {
    super(context, R.layout.friend_row, null, 0);
    this.context = context;
    this.touchListener = touchListener;
  }

  static class ViewHolder {
    long id;
    String number;
    String contactName;
    TextView contactNameView;
    TextView lastContactView;
    IconTextView frequencyIconView;
    View callBackdropView;
    View deleteBackdropView;
    RelativeLayout frontView;
  }

  @Override
  public void bindView(final View view, final Context context, final Cursor cursor) {
    ViewHolder holder = (ViewHolder) view.getTag();
    if (holder == null) {
      holder = new ViewHolder();
      holder.contactNameView = (TextView) view.findViewById(R.id.contact_name);
      holder.lastContactView = (TextView) view.findViewById(R.id.last_contact);
      holder.frequencyIconView = (IconTextView) view.findViewById(R.id.frequency_icon);
      holder.callBackdropView = view.findViewById(R.id.call_backdrop);
      holder.deleteBackdropView = view.findViewById(R.id.delete_backdrop);
      holder.frontView = (RelativeLayout) view.findViewById(R.id.front);

      holder.frontView.setOnTouchListener(touchListener);
      view.setTag(holder);
    }

    holder.id = cursor.getLong(cursor.getColumnIndex(FriendEntry._ID));
    // TODO: fix dial!
//    holder.number = cursor.getString(cursor.getColumnIndex(FriendEntry.NUMBER));
    holder.contactName = getContactName(cursor);
    holder.contactNameView.setText(holder.contactName);

    final long lastContact = cursor.getLong(cursor.getColumnIndex(FriendEntry.LAST_CONTACT));
    final CharSequence text = Util.getRelativeTime(lastContact, "too long ago!");
    holder.lastContactView.setText(text);

    final long frequency = cursor.getLong(cursor.getColumnIndex(FriendEntry.FREQUENCY));
    final long now = System.currentTimeMillis();
    if (lastContact < 0 || lastContact + frequency <= now) {
      holder.frequencyIconView.setTextColor(RED);
    } else {
      final float scale = (now - lastContact) / (float) frequency;
      holder.frequencyIconView.setTextColor(getColorIndicator(scale));
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
    if (scale < 0.5) {
      color = GREEN;
    } else if (scale < 0.9) {
      color = ORANGE;
    }
    return color;
  }

}
