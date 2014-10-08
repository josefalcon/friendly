package falcon.com.friendly;

import android.telephony.PhoneNumberUtils;
import android.text.format.DateUtils;

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

  public static String inClausePlaceholders(final int count) {
    final StringBuilder sb = new StringBuilder("(");
    final int posts = count - 1;
    for (int i = 0; i < count; i++) {
      sb.append("?");
      if (i < posts) {
        sb.append(",");
      }
    }
    sb.append(")");
    return sb.toString();
  }
}
