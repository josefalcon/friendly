package falcon.com.friendly.resolver;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.util.Log;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CallLogResolver implements Iterable<CallLogResolver.Entry>{

  private final ContentResolver contentResolver;
  private Set<String> numbersFilter;

  public CallLogResolver(final ContentResolver contentResolver) {
    this.contentResolver = contentResolver;
  }

  /**
   * Creates a new CallLogResolver with the given ContentResolver.
   *
   * @param contentResolver a non-null ContentResolver.
   */
  public static CallLogResolver from(final ContentResolver contentResolver) {
    return new CallLogResolver(contentResolver);
  }

  /**
   * Limits this resolve to only the numbers in the given collection.
   *
   * @param numbers the non-null collection of numbers to filter by.
   */
  public CallLogResolver filter(final Collection<String> numbers) {
    if (numbers == null) {
      throw new NullPointerException("numbers may not be null");
    }

    this.numbersFilter = new HashSet<>(numbers);
    return this;
  }

  public Cursor getCursor() {
    return contentResolver.query(CallLog.Calls.CONTENT_URI,
                                 PROJECTION,
                                 getSelection(),
                                 getSelectionArgs(),
                                 null);
  }

  @Override
  public Iterator<Entry> iterator() {

    return new Iterator<Entry>() {
      final Cursor cursor = getCursor();

      @Override
      public boolean hasNext() {
        return cursor.moveToNext();
      }

      @Override
      public Entry next() {
        final Entry entry = new Entry();
        entry.duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION));
        entry.number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
        entry.timestamp = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
        return entry;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };

  }

  private static final String[] PROJECTION = new String[] {
    CallLog.Calls._ID,
    CallLog.Calls.DURATION,
    CallLog.Calls.NUMBER,
    CallLog.Calls.DATE,
  };

  /**
   * Creates a new selection string with a placehold for each entry in the filtered numbers set.
   *
   * TODO: this is pretty hacky.
   *
   * @return a new selection string.
   */
  private String getSelection() {
    if (numbersFilter == null || numbersFilter.isEmpty()) {
      return null;
    }

    final StringBuilder sb = new StringBuilder(CallLog.Calls.NUMBER + " IN (");
    for (int i = 0; i < numbersFilter.size(); i++) {
      sb.append("?");
      if (i != numbersFilter.size() - 1) {
        sb.append(",");
      }
    }
    sb.append(") AND ")
      .append(CallLog.Calls.DURATION)
      .append(" > 300");

    final String s = sb.toString();
    Log.d("CallLogResolver", s);
    return s;
  }

  /**
   * Converts the current filtered numbers set to an array of arguments.
   *
   * @return a new String array containing the filtered numbers
   */
  private String[] getSelectionArgs() {
    if (numbersFilter == null || numbersFilter.isEmpty()) {
      return null;
    }
    return numbersFilter.toArray(new String[numbersFilter.size()]);
  }

  public long getLastContact(final String number) {
    final Uri uri = Uri.withAppendedPath(CallLog.Calls.CONTENT_FILTER_URI, Uri.encode(number));

    final String selection = CallLog.Calls.DURATION + " > 1";
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

  /**
   * POJO for a CallLog entry.
   */
  public static class Entry {

    public long duration;

    public String number;

    public long timestamp;

    public Entry() {
    }

    public Entry(final long duration, final String number, final long timestamp) {
      this.duration = duration;
      this.number = number;
      this.timestamp = timestamp;
    }

    @Override
    public String toString() {
      return "Entry{" +
             "duration=" + duration +
             ", number='" + number + '\'' +
             ", timestamp=" + timestamp +
             '}';
    }
  }

}
