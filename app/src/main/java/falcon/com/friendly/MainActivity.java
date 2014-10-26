package falcon.com.friendly;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import java.util.Map;

import falcon.com.friendly.dialog.FriendDialog;
import falcon.com.friendly.dialog.FriendDialogListener;
import falcon.com.friendly.fragment.FriendListFragment;
import falcon.com.friendly.resolver.CallLogResolver;
import falcon.com.friendly.resolver.ContactResolver;
import falcon.com.friendly.service.AlarmService;
import falcon.com.friendly.store.FriendContract;
import falcon.com.friendly.store.FriendlyDatabaseHelper;

import static android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE;
import static android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
import static android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER;

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
      Log.d(T, contactUri.toString());
      final Bundle contact = contactResolver.getContact(contactUri);

      final long lastContact = callLogResolver.getLastContact(contact.getString(NUMBER));
      contact.putLong(FriendContract.FriendEntry.LAST_CONTACT, lastContact);

      final FriendDialog friendDialog = new FriendDialog();
      friendDialog.setArguments(contact);
      friendDialog.show(getFragmentManager(), "FriendDialog");
    }
  }

  @Override
  public void onDialogPositiveClick(final DialogFragment dialog) {
    Log.d(T, "Positive FriendDialog click");
    final Bundle contact = dialog.getArguments();
    final ContentValues contentValues = contactResolver.getFriendContentValues(contact);
    if (saveFriend(contentValues)) {
      Log.d(T, "Saved friend");
      String name = contact.getString(DISPLAY_NAME);
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
   * Persists the given ContentValues to the Friend database table.
   *
   * @param contentValues the ContentValues representing a Friend.
   */
  private boolean saveFriend(final ContentValues contentValues) {
    Log.d(T, "Saving friend...");
    final FriendlyDatabaseHelper helper = FriendlyDatabaseHelper.getInstance(this);
    final SQLiteDatabase db = helper.getWritableDatabase();
    try {

      for (Map.Entry<String, Object> e : contentValues.valueSet()) {
        Log.d(T, e.getKey() + " : " + e.getValue());
      }

      final long id = db.replaceOrThrow(FriendContract.FriendEntry.TABLE, null, contentValues);
      final boolean modified = id != -1;
      if (modified) {
        final Intent intent = new Intent(this, AlarmService.class);
        startService(intent);
      }
      return modified;
    } catch (final SQLException e) {
      Log.d(T, "Ignoring duplicate friend...");
    }
    return false;
  }
}
