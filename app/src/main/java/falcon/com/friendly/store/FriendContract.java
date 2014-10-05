package falcon.com.friendly.store;

import android.provider.BaseColumns;

public final class FriendContract {

  private FriendContract() {
  }

  public static abstract class FriendEntry implements BaseColumns {

    public static final String TABLE = "friend";

    /**
     * The unique phone number associated with this friend.
     * <P>Type: TEXT</P>
     */
    public static final String NUMBER = "number";

    /**
     * The phone type of the phone number.
     * <P>Type: INTEGER</P>
     */
    public static final String TYPE = "type";

    /**
     * The contact id associated with this friend.
     * <P>Type: INTEGER</P>
     */
    public static final String CONTACT_ID = "contact_id";

    /**
     * The contact lookup key associated with this friend.
     * <P>Type: TEXT</P>
     */
    public static final String LOOKUP_KEY = "lookup_key";

    /**
     * The date of the last contact with this friend, stored as the number of milliseconds since
     * epoch.
     * <P>Type: INTEGER</P>
     */
    public static final String LAST_CONTACT = "last_contact";

    /**
     * The intended frequency of contact, stored in milliseconds.
     * <P>Type: INTEGER</P>
     */
    public static final String FREQUENCY = "frequency";
  }

}
