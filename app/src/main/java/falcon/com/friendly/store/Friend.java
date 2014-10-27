package falcon.com.friendly.store;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import falcon.com.friendly.Util;

/**
 * POJO representing a Friend.
 */
public class Friend implements Parcelable {

  public static final Creator<Friend> CREATOR = new Creator<Friend>() {
    @Override
    public Friend createFromParcel(final Parcel source) {
      return new Friend(source);
    }

    @Override
    public Friend[] newArray(final int size) {
      return new Friend[size];
    }
  };

  /**
   * The id of this friend if it exists.
   */
  public Long id;

  /**
   * The contact id associated with this friend.
   */
  public Long contactId;

  /**
   * The contact lookup key associated with this friend.
   */
  public String lookupKey;

  /**
   * The display name of the friend.
   */
  public String displayName;

  /**
   * The date of the last contact with this friend, stored in milliseconds.
   */
  public Long lastContact;

  /**
   * The intended frequency of contact, stored in milliseconds.
   */
  public Long frequency;

  /**
   * The phone numbers associated with this friend.
   */
  public List<Phone> numbers;

  public Friend() {
  }

  public Friend(final Parcel parcel) {
    id = parcel.readLong();
    contactId = parcel.readLong();
    lookupKey = parcel.readString();
    displayName = parcel.readString();
    lastContact = parcel.readLong();
    frequency = parcel.readLong();
    numbers = new ArrayList<>();
    parcel.readTypedList(numbers, Phone.CREATOR);
  }

  /**
   * Merges the details of the given friend into this friend.
   *
   * @param other the friend to merge in
   */
  public void merge(final Friend other) {
    contactId = other.contactId;
    lookupKey = other.lookupKey;
    displayName = other.displayName;
    lastContact = Util.max(lastContact, other.lastContact);
    frequency = Util.min(frequency, other.frequency);

    if (other.numbers != null && !other.numbers.isEmpty()) {
      if (numbers == null) {
        numbers = new ArrayList<>();
      }
      numbers.addAll(other.numbers);
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(final Parcel dest, final int flags) {
    dest.writeValue(id);
    dest.writeValue(contactId);
    dest.writeValue(lookupKey);
    dest.writeValue(displayName);
    dest.writeValue(lastContact);
    dest.writeValue(frequency);
    dest.writeValue(numbers);
  }
}
