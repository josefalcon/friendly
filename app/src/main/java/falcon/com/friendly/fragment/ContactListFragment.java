package falcon.com.friendly.fragment;


import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import falcon.com.friendly.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ContactListFragment extends Fragment {

  private ListView listView;

  public ContactListFragment() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.contact_list_fragment, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    this.listView = (ListView) getActivity().findViewById(R.id.contactListView);

    final SimpleCursorAdapter adapter =
      new SimpleCursorAdapter(getActivity(),
                              R.layout.contact_list_item,
                              getContactCursor(),
                              new String[]{ContactsContract.Contacts.PHOTO_URI,
                                           ContactsContract.Contacts.DISPLAY_NAME},
                              new int[]{R.id.imageView, R.id.text1},
                              0);

    this.listView.setAdapter(adapter);
    this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("ContactListFragment", position + "");
      }
    });
  }

  private static final String[] PROJECTION = new String[] {
    ContactsContract.Contacts._ID,
    ContactsContract.Contacts.PHOTO_URI,
    ContactsContract.Contacts.DISPLAY_NAME
  };

  private Cursor getContactCursor() {
    return getActivity().getContentResolver()
      .query(ContactsContract.Contacts.CONTENT_URI,
             PROJECTION, null, null, null);
  }

}
