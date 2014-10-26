package falcon.com.friendly.extra;

import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * A cursor wrapper for handling deleted items. Delays deletion, suitable for swiping.
 *
 * http://stackoverflow.com/questions/15468100/cursoradapter-backed-listview-delete-animation-flickers-on-delete
 */
public class DeleteCursorWrapper extends CursorWrapper {

  private int mVirtualPosition;
  private final int mHiddenPosition;

  public DeleteCursorWrapper(final Cursor cursor, final int hiddenPosition) {
    super(cursor);
    mVirtualPosition = -1;
    mHiddenPosition = hiddenPosition;
  }

  @Override
  public int getCount() {
    return super.getCount() - 1;
  }

  @Override
  public int getPosition() {
    return mVirtualPosition;
  }

  @Override
  public boolean move(final int offset) {
    return moveToPosition(getPosition() + offset);
  }

  @Override
  public boolean moveToFirst() {
    return moveToPosition(0);
  }

  @Override
  public boolean moveToLast() {
    return moveToPosition(getCount() - 1);
  }

  @Override
  public boolean moveToNext() {
    return moveToPosition(getPosition() + 1);
  }

  @Override
  public boolean moveToPosition(final int position) {
    mVirtualPosition = position;
    int cursorPosition = position;
    if (cursorPosition >= mHiddenPosition) {
      cursorPosition++;
    }
    return super.moveToPosition(cursorPosition);
  }

  @Override
  public boolean moveToPrevious() {
    return moveToPosition(getPosition() - 1);
  }

  @Override
  public boolean isBeforeFirst() {
    return getPosition() == -1 || getCount() == 0;
  }

  @Override
  public boolean isFirst() {
    return getPosition() == 0 && getCount() != 0;
  }

  @Override
  public boolean isLast() {
    final int count = getCount();
    return getPosition() == (count - 1) && count != 0;
  }

  @Override
  public boolean isAfterLast() {
    final int count = getCount();
    return getPosition() == count || count == 0;
  }
}
