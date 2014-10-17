package falcon.com.friendly;

import android.telephony.PhoneNumberUtils;
import android.text.format.DateUtils;

import java.util.Calendar;

/**
 * Utility functions.
 */
public final class Util {

  private Util() {
  }

  /**
   * Returns a relative time string for the given time in milliseconds. If the given time is invalid
   * returns the given default string.
   *
   * @param timeInMillis  the time to convert
   * @param defaultString a fall back value
   * @return a relative time string
   */
  public static String getRelativeTime(final long timeInMillis,
                                       final String defaultString) {
    final CharSequence text;
    if (timeInMillis <= 0) {
      text = defaultString;
    } else {
      text = DateUtils.getRelativeTimeSpanString(timeInMillis,
                                                 System.currentTimeMillis(),
                                                 DateUtils.MINUTE_IN_MILLIS);
    }
    return text.toString();
  }

  /**
   * Strips separators and formats the given phone number into a canonical form.
   *
   * @param rawNumber the raw number to clean
   * @return a canonical phone number
   */
  public static String cleanPhoneNumber(final String rawNumber) {
    return PhoneNumberUtils.stripSeparators(PhoneNumberUtils.formatNumber(rawNumber));
  }

  /**
   * Rests the given calendar object's time to 0.
   *
   * @param calendar the object to modify
   */
  public static void resetCalendarTime(final Calendar calendar) {
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
  }
}
