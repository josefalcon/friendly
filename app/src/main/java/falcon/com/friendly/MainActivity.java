package falcon.com.friendly;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import falcon.com.friendly.fragment.FriendFragment;


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
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    final int id = item.getItemId();
    if (id == R.id.action_settings) {
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

  public void startNewFriendActivity(final View view) {
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
