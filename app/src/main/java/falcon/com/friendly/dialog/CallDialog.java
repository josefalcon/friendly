package falcon.com.friendly.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.Html;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import falcon.com.friendly.store.Phone;

/**
 * A dialog shown when attempting to call a contact with more than one phone number.
 */
public class CallDialog extends DialogFragment {

  public static final String T = "CallDialog";

  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    final ArrayList<Phone> numbers = getArguments().getParcelableArrayList("phoneNumbers");
    final CharSequence[] formattedNumbers = formatPhoneNumbers(numbers);

    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("Choose number")
      .setItems(formattedNumbers, new DialogInterface.OnClickListener() {
        public void onClick(final DialogInterface dialog, final int which) {
          final Phone phone = numbers.get(which);
          Log.d(T, "Dialing friend: " + phone.number);

          final Intent intent = new Intent(Intent.ACTION_DIAL);
          intent.setData(Uri.parse("tel:" + phone.number));
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(intent);
        }
      });
    return builder.create();
  }

  private static final String FORMAT = "<br>%s<br>"
                                       + "<font color='#747474'>%s</font><br>";

  private CharSequence[] formatPhoneNumbers(final List<Phone> numbers) {
    final CharSequence[] results = new CharSequence[numbers.size()];
    for (int i = 0; i < results.length; i++) {
      final Phone phone = numbers.get(i);
      final String formattedNumber = PhoneNumberUtils.formatNumber(phone.number);
      final String phoneType = getPhoneTypeString(phone.type);
      results[i] = Html.fromHtml(String.format(FORMAT, formattedNumber, phoneType));
    }
    return results;
  }

  private String getPhoneTypeString(final int phoneType) {
    return getResources()
      .getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(phoneType));
  }

}
