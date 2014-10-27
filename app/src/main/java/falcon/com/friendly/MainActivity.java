package falcon.com.friendly;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.cocosw.undobar.UndoBarController;
import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import falcon.com.friendly.dialog.FriendDialog;
import falcon.com.friendly.dialog.FriendDialogListener;
import falcon.com.friendly.fragment.FriendListFragment;
import falcon.com.friendly.resolver.CallLogResolver;
import falcon.com.friendly.resolver.ContactResolver;
import falcon.com.friendly.service.AlarmService;
import falcon.com.friendly.store.Friend;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE;

public class MainActivity extends Activity implements FriendDialogListener {

  private static final String T = "MainActivity";

  private FriendListFragment friendListFragment;

  private ContactResolver contactResolver;

  private CallLogResolver callLogResolver;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    contactResolver = new ContactResolver(getContentResolver());
    callLogResolver = new CallLogResolver(getContentResolver());
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    menu.findItem(R.id.action_new_friend).setIcon(
      new IconDrawable(this, Iconify.IconValue.fa_plus)
        .colorRes(R.color.white)
        .actionBarSize());

    // hide the settings menu
    menu.findItem(R.id.action_settings).setVisible(false);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    final int id = item.getItemId();
    if (id == R.id.action_new_friend) {
      showContactPicker();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onAttachFragment(final Fragment fragment) {
    super.onAttachFragment(fragment);
    if (fragment instanceof FriendListFragment) {
      friendListFragment = (FriendListFragment) fragment;
    }
  }

  private static final int PICK_CONTACT_REQUEST = 1;

  /**
   * Shows the contact picker as the first step of adding a new friend.
   */
  private void showContactPicker() {
    final Intent pickContactIntent =
      new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    pickContactIntent.setType(CONTENT_TYPE);
    startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
  }

  @Override
  protected void onActivityResult(final int requestCode,
                                  final int resultCode,
                                  final Intent data) {
    if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
      final Uri contactUri = data.getData();
      final Friend newFriend = contactResolver.getContactAsFriend(contactUri);
      final FriendlyDatabaseHelper db = FriendlyDatabaseHelper.getInstance(this);
      final Friend oldFriend = db.getFriend(newFriend.contactId, newFriend.lookupKey);
      if (oldFriend != null) {
        // merge in the new details if the contact already exists
        oldFriend.merge(newFriend);
      }

      final FriendDialog friendDialog = new FriendDialog();
      final Bundle args = new Bundle(1);
      args.putParcelable("friend", (oldFriend != null) ? oldFriend : newFriend);
      friendDialog.setArguments(args);
      friendDialog.show(getFragmentManager(), "FriendDialog");
    }
  }

  @Override
  public void onDialogPositiveClick(final DialogFragment dialog) {
    final Bundle contact = dialog.getArguments();
    final Friend friend = contact.getParcelable("friend");
    if (saveFriend(friend)) {
      String name = friend.displayName;
      if (name == null || name.isEmpty()) {
        name = "Contact";
      }

      final UndoBarController.UndoBar undoBar =
        new UndoBarController.UndoBar(this).style(UndoBarController.MESSAGESTYLE);
      undoBar.message(name + " saved");
      undoBar.show();
      friendListFragment.refresh();
    }
  }

  @Override
  public void onDialogNegativeClick(final DialogFragment dialog) {
    Log.d(T, "Cancelling FriendDialog");
  }

  /**
   * Persists the given Friend to the Friend database table.
   *
   * @param friend the friend to save
   */
  private boolean saveFriend(final Friend friend) {
    Log.d(T, "Saving friend");
    final FriendlyDatabaseHelper helper = FriendlyDatabaseHelper.getInstance(this);
    final boolean modified;
    if (friend.id == null) {
      modified = helper.createFriend(friend);
    } else {
      modified = helper.updateFriend(friend);
    }

    if (modified) {
      final Intent intent = new Intent(this, AlarmService.class);
      startService(intent);
    }
    return modified;
  }

}
