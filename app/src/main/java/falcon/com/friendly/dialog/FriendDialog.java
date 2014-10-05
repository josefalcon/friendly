package falcon.com.friendly.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import falcon.com.friendly.R;
import falcon.com.friendly.TimeUnit;
import falcon.com.friendly.Util;
import falcon.com.friendly.store.FriendContract;

import static android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
import static android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER;
import static android.provider.ContactsContract.CommonDataKinds.Phone.TYPE;

public class FriendDialog extends DialogFragment {

  private FriendDialogListener friendDialogListener;

  @Override
  public void onAttach(final Activity activity) {
    super.onAttach(activity);
    try {
      friendDialogListener = (FriendDialogListener) activity;
    } catch (final ClassCastException e) {
      throw new ClassCastException(activity.toString()
                                   + " must implement FriendDialogListener");
    }
  }

  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    final Activity activity = getActivity();
    final Bundle arguments = getArguments();

    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    final LayoutInflater inflater = activity.getLayoutInflater();
    final View view = inflater.inflate(R.layout.activity_new_friend, null);

    final TextView contactNameView = (TextView) view.findViewById(R.id.contact_name);
    final TextView contactPhoneView = (TextView) view.findViewById(R.id.contact_phone);
    final TextView contactPhoneTypeView = (TextView) view.findViewById(R.id.contact_phone_type);
    final TextView lastContactView = (TextView) view.findViewById(R.id.last_contact);
    final EditText quantityView = (EditText) view.findViewById(R.id.quantity);
    final Spinner timeUnitSpinner = (Spinner) view.findViewById(R.id.time_unit);

    builder.setView(view)
      .setPositiveButton("Save", new DialogInterface.OnClickListener() {
        public void onClick(final DialogInterface dialog, final int id) {
          final int quantity = Integer.parseInt(quantityView.getText().toString());
          final TimeUnit timeUnit = TimeUnit.getByIndex(timeUnitSpinner.getSelectedItemPosition());
          final long frequency = timeUnit.inMilliseconds(quantity);

          if (arguments != null && frequency > 0) {
            arguments.putLong(FriendContract.FriendEntry.FREQUENCY, frequency);
          }

          friendDialogListener.onDialogPositiveClick(FriendDialog.this);
        }
      })
      .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(final DialogInterface dialog, final int id) {
          friendDialogListener.onDialogNegativeClick(FriendDialog.this);
        }
      });

    final AlertDialog alertDialog = builder.create();
    alertDialog.setCanceledOnTouchOutside(false);

    final String[] timeUnitStrings = TimeUnit.stringValues();
    final ArrayAdapter<CharSequence> timeUnitAdapter =
      new ArrayAdapter<CharSequence>(activity,
                                     android.R.layout.simple_spinner_item,
                                     timeUnitStrings);
    timeUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    timeUnitSpinner.setAdapter(timeUnitAdapter);

    final long frequency = arguments.getLong(FriendContract.FriendEntry.FREQUENCY);
    if (frequency > 0) {
      int i = 0;
      TimeUnit bestUnit = TimeUnit.DAYS;
      final TimeUnit[] values = TimeUnit.values();
      for (i = values.length - 1; i >= 0; i--) {
        final TimeUnit unit = values[i];
        if (frequency % unit.getMilliseconds() == 0) {
          bestUnit = unit;
          break;
        }
      }

      final long quantity = frequency / bestUnit.getMilliseconds();
      quantityView.setText(String.valueOf(quantity));
      timeUnitSpinner.setSelection(i);
    }


    contactNameView.setText(arguments.getString(DISPLAY_NAME));
    contactPhoneView.setText(PhoneNumberUtils.formatNumber(arguments.getString(NUMBER)));
    contactPhoneTypeView.setText(getPhoneTypeString(arguments.getInt(TYPE)) + " ");

    final long lastContact = arguments.getLong(FriendContract.FriendEntry.LAST_CONTACT);
    lastContactView.setText(Util.getRelativeTime(lastContact, "too long ago!"));
    return alertDialog;
  }

  private String getPhoneTypeString(final int phoneType) {
    return getResources()
      .getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(phoneType));
  }

}
