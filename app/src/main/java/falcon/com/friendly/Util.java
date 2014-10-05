package falcon.com.friendly;

import android.telephony.PhoneNumberUtils;
import android.text.format.DateUtils;

public final class Util {

  private Util() {
  }

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
}
