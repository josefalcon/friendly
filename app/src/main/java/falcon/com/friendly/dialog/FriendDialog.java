package falcon.com.friendly.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import falcon.com.friendly.R;
import falcon.com.friendly.TimeUnit;
import falcon.com.friendly.Util;
import falcon.com.friendly.store.Friend;
import falcon.com.friendly.store.Phone;

/**
 * A dialog shown when adding/modifying a friend.
 */
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

  private static final String PHONE_NUMBER_FORMAT = "<strong>%s</strong> %s";

  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    final Activity activity = getActivity();
    final Friend friend = getArguments().getParcelable("friend");

    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    final LayoutInflater inflater = activity.getLayoutInflater();
    final View view = inflater.inflate(R.layout.activity_new_friend, null);

    final LinearLayout contactHeader = (LinearLayout) view.findViewById(R.id.contact_header);
    final TextView contactNameView = (TextView) view.findViewById(R.id.contact_name);
    final TextView lastContactView = (TextView) view.findViewById(R.id.last_contact);
    final EditText quantityView = (EditText) view.findViewById(R.id.quantity);
    final Spinner timeUnitSpinner = (Spinner) view.findViewById(R.id.time_unit);

    builder.setView(view)
      .setPositiveButton("Save", new DialogInterface.OnClickListener() {
        public void onClick(final DialogInterface dialog, final int id) {
          final int quantity = Integer.parseInt(quantityView.getText().toString());
          final TimeUnit timeUnit = TimeUnit.getByIndex(timeUnitSpinner.getSelectedItemPosition());
          final long frequency = timeUnit.inMilliseconds(quantity);

          if (friend != null && frequency > 0) {
            friend.frequency = frequency;
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

    final Long frequency = friend.frequency;
    if (frequency != null && frequency > 0) {
      int i;
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

    contactNameView.setText(friend.displayName);
    lastContactView.setText(Util.getRelativeTime(friend.lastContact, "too long ago!"));

    for (final Phone phone : friend.numbers) {
      final String formattedNumber = PhoneNumberUtils.formatNumber(phone.number);
      final String phoneTypeString = getPhoneTypeString(phone.type);
      final String phoneNumberHtml =
        String.format(PHONE_NUMBER_FORMAT, phoneTypeString, formattedNumber);

      final TextView phoneNumberView = new TextView(getActivity());
      phoneNumberView.setText(Html.fromHtml(phoneNumberHtml));
      phoneNumberView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Small);
      phoneNumberView.setTextColor(getResources().getColor(R.color.gray));
      contactHeader.addView(phoneNumberView);
    }
    return alertDialog;
  }

  private String getPhoneTypeString(final int phoneType) {
    return getResources()
      .getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(phoneType));
  }

}
