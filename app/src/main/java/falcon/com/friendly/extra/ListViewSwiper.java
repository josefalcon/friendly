package falcon.com.friendly.extra;

import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;

/**
 * Class for managing list item swipes in a ListView.
 *
 * Code based on: https://www.youtube.com/watch?v=YCHNAi9kJI4
 */
public class ListViewSwiper {

  private static final String T = "ListViewSwiper";

  private static final int LEFT = -1;
  private static final int RIGHT = 1;

  public static interface SwipeListener {

    /**
     * Called when the given view moves to the left.
     *
     * @param view the swiped view
     */
    void onMoveLeft(View view);

    /**
     * Called when the given view was successfully swiped left.
     *
     * @param view the swiped view
     */
    void onSwipedLeft(View view);

    /**
     * Called when the given view moves to the right.
     *
     * @param view the swiped view
     */
    void onMoveRight(View view);

    /**
     * Called when the given view was successfully swiped right.
     *
     * @param view the swiped view
     */
    void onSwipedRight(View view);

  }

  // Swipe configuration
  private final int swipeSlop;
  private final int minFlingVelocity;
  private final int maxFlingVelocity;

  // Swiping state
  private boolean isSwiping = false;
  private boolean isItemPressed = false;

  // View objects
  private final ListView listView;

  private final SwipeListener swipeListener;

  public ListViewSwiper(final ListView listView, final SwipeListener swipeListener) {
    this.listView = listView;
    this.swipeListener = swipeListener;

    final ViewConfiguration viewConfiguration = ViewConfiguration.get(listView.getContext());
    swipeSlop = viewConfiguration.getScaledTouchSlop();
    minFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
    maxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
  }

  private View.OnTouchListener singleton;

  /**
   * Returns an instance of an OnTouchListener.
   *
   * @return an OnTouchListener
   */
  public View.OnTouchListener getOnTouchListener() {
    if (singleton == null) {
      singleton = new SwipeTouchListener();
    }
    return singleton;
  }

  /**
   * Listener for handling swipe gestures on list items.
   */
  private class SwipeTouchListener implements View.OnTouchListener {

    private float downX;
    private int direction;
    private VelocityTracker velocityTracker;

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
      switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          if (actionDown(event)) {
            return false;
          }
          break;
        case MotionEvent.ACTION_CANCEL:
          actionCancel(v);
          break;
        case MotionEvent.ACTION_MOVE:
          actionMove(v, event);
          break;
        case MotionEvent.ACTION_UP:
          actionUp(v, event);
          break;
        default:
          return false;
      }
      return true;
    }

    private void actionUp(final View v, final MotionEvent event) {
      if (isSwiping && velocityTracker != null) {
        velocityTracker.addMovement(event);

        final float x = event.getX() + v.getTranslationX();
        final float deltaX = x - downX;
        final float deltaXAbs = Math.abs(deltaX);
        final float fractionCovered;
        final float endX;
        final boolean remove;

        velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
        final float velocityX = Math.abs(velocityTracker.getXVelocity());
        final float velocityY = Math.abs(velocityTracker.getYVelocity());

        if (deltaXAbs > v.getWidth() * 0.3 || isFling(velocityX, velocityY)) {
          // swiped more than enough...remove it
          fractionCovered = deltaXAbs / v.getWidth();
          endX = deltaX < 0 ? -v.getWidth() : v.getWidth();
          remove = true;
        } else {
          // reset position, not a fling or a far enough swipe
          fractionCovered = 1 - (deltaXAbs / v.getWidth());
          endX = 0;
          remove = false;
        }

        final long duration = (int) ((1 - fractionCovered) * 250);
        listView.setEnabled(false);
        v.animate()
          .setDuration(duration)
          .translationX(endX)
          .withEndAction(new Runnable() {
            @Override
            public void run() {
              // restore
              v.setTranslationX(0);
              if (remove) {
                if (direction == LEFT) {
                  swipeListener.onSwipedLeft(v);
                } else if (direction == RIGHT) {
                  swipeListener.onSwipedRight(v);
                }
              }

              isSwiping = false;
              listView.setEnabled(true);
            }
          });
      } else {
        final int position = listView.getPositionForView(v);
        final long id = listView.getItemIdAtPosition(position);
        listView.performItemClick(v, position, id);
      }
      isItemPressed = false;
    }

    private boolean isFling(final float velocityX, final float velocityY) {
      return minFlingVelocity <= velocityX
                 && velocityX <= maxFlingVelocity
                 && velocityY * 2 < velocityX;
    }

    private void actionMove(final View v, final MotionEvent event) {
      if (velocityTracker == null) {
        return;
      }

      velocityTracker.addMovement(event);
      final float x = event.getX() + v.getTranslationX();
      final float deltaX = x - downX;
      final float deltaXAbs = Math.abs(deltaX);

      if (deltaX < 0) {
        direction = LEFT;
        swipeListener.onMoveLeft(v);
      } else {
        direction = RIGHT;
        swipeListener.onMoveRight(v);
      }

      if (!isSwiping) {
        if (deltaXAbs > swipeSlop) {
          isSwiping = true;
          listView.requestDisallowInterceptTouchEvent(true);
        }
      }

      if (isSwiping) {
        v.setTranslationX(deltaX);
      }
    }

    private void actionCancel(final View v) {
      v.setTranslationX(0);
      isItemPressed = false;
      velocityTracker = null;
      downX = 0;
    }

    private boolean actionDown(final MotionEvent event) {
      if (isItemPressed) {
        // one item at a time
        return true;
      }
      velocityTracker = VelocityTracker.obtain();
      velocityTracker.addMovement(event);
      isItemPressed = true;
      downX = event.getX();
      return false;
    }
  }

}
