package falcon.com.friendly.dialog;

import android.app.DialogFragment;

/**
 * Interface for handling call backs from a FriendDialog.
 */
public interface FriendDialogListener {
  void onDialogPositiveClick(DialogFragment dialog);
  void onDialogNegativeClick(DialogFragment dialog);
}
