package falcon.com.friendly.store;


import android.os.Parcel;
import android.os.Parcelable;

public class Phone implements Parcelable {

  public static final Creator<Phone> CREATOR = new Creator<Phone>() {
    @Override
    public Phone createFromParcel(final Parcel source) {
      return new Phone(source);
    }

    @Override
    public Phone[] newArray(final int size) {
      return new Phone[size];
    }
  };

  /**
   * The id of this phone number if it exists.
   */
  public Long id;

  /**
   * The phone number.
   */
  public String number;

  /**
   * The type of phone.
   */
  public Integer type;

  public Phone() {
  }

  public Phone(final Parcel parcel) {
    id = parcel.readLong();
    number = parcel.readString();
    type = parcel.readInt();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(final Parcel dest, final int flags) {
    dest.writeValue(id);
    dest.writeValue(number);
    dest.writeValue(type);
  }
}
