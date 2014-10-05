package falcon.com.friendly.dialog;

import android.app.DialogFragment;

public interface FriendDialogListener {
  void onDialogPositiveClick(DialogFragment dialog);
  void onDialogNegativeClick(DialogFragment dialog);
}
