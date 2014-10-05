package falcon.com.friendly;

public enum TimeUnit {
  DAYS(86400000L),
  WEEKS(604800000L),
  MONTHS(2629740000L);

  private static final String[] TIME_UNIT_STRINGS = new String[values().length];
  static {
    final TimeUnit[] values = values();
    for (int i = 0; i < TIME_UNIT_STRINGS.length; i++) {
      TIME_UNIT_STRINGS[i] = values[i].name().toLowerCase();
    }
  }

  private final long milliseconds;

  private TimeUnit(final long milliseconds) {
    this.milliseconds = milliseconds;
  }

  public long getMilliseconds() {
    return milliseconds;
  }

  public long inMilliseconds(final int multiplier) {
    return multiplier * milliseconds;
  }

  public static String[] stringValues() {
    return TIME_UNIT_STRINGS;
  }

  public static TimeUnit getByIndex(final int index) {
    if (index >= 0 && index < TIME_UNIT_STRINGS.length) {
      return values()[index];
    }
    return null;
  }
}
