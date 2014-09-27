package falcon.com.friendly;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import falcon.com.friendly.fragment.FriendFragment;
import falcon.com.friendly.resolver.CallLogResolver;
import falcon.com.friendly.service.CallLogUpdateService;


public class MainActivity extends Activity {

  private static final String T = "MainActivity";

  private FriendFragment friendFragment;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);

    menu.findItem(R.id.action_new_friend).setIcon(
      new IconDrawable(this, Iconify.IconValue.fa_plus)
        .colorRes(R.color.white)
        .actionBarSize());

    // hide the settings menu
    menu.findItem(R.id.action_settings)
      .setVisible(false);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    final int id = item.getItemId();
    if (id == R.id.action_new_friend) {
      startNewFriendActivity();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onAttachFragment(final Fragment fragment) {
    super.onAttachFragment(fragment);
    if (fragment instanceof FriendFragment) {
      friendFragment = (FriendFragment) fragment;
    }
  }

  private void startNewFriendActivity() {
    final Intent intent = new Intent(this, NewFriendActivity.class);
    startActivityForResult(intent, 1);
  }

  @Override
  protected void onActivityResult(final int requestCode,
                                  final int resultCode,
                                  final Intent data) {
    if (resultCode == RESULT_OK && friendFragment != null) {
      Log.d(T, "NewFriendActivity added a new friend. Updating view...");
      friendFragment.refresh();
    }
  }
}
