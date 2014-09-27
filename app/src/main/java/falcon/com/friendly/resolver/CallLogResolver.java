package falcon.com.friendly.resolver;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;

public class CallLogResolver {

  private final ContentResolver contentResolver;

  public CallLogResolver(final ContentResolver contentResolver) {
    this.contentResolver = contentResolver;
  }

  public long getLastContact(final String number) {
    final Uri uri = Uri.withAppendedPath(CallLog.Calls.CONTENT_FILTER_URI, Uri.encode(number));

    final String selection = CallLog.Calls.DURATION + " > 300";
    final Cursor cursor = contentResolver.query(uri,
                                               new String[]{CallLog.Calls.DATE},
                                               selection,
                                               null,
                                               CallLog.Calls.DATE + " DESC");
    try {
      if (cursor.moveToFirst()) {
        return cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
      }
    } finally {
      cursor.close();
    }
    return -1;
  }

}
