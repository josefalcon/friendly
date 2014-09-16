package falcon.com.friendly.fragment;


import android.app.Fragment;
import android.os.Bundle;
import android.provider.CallLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import falcon.com.friendly.R;
import falcon.com.friendly.resolver.CallLogResolver;

/**
 * A simple {@link Fragment} subclass.
 */
public class CallLogFragment extends Fragment {

  private CallLogResolver callLogResolver;

  private ListView listView;

  public CallLogFragment() {
    // Required empty public constructor
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d("CallLogFragment", "onCreate invoked");
    this.callLogResolver = CallLogResolver.from(getActivity().getContentResolver());
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
    Log.d("CallLogFragment", "onCreateView invoked");
    for (CallLogResolver.Entry entry : callLogResolver) {
      Log.d("CallLogFragment", entry.toString());
    }
    return inflater.inflate(R.layout.call_log_fragment, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    this.listView = (ListView) getActivity().findViewById(R.id.callLogListView);

    final SimpleCursorAdapter adapter =
      new SimpleCursorAdapter(getActivity(),
                              R.layout.call_log_item,
                              this.callLogResolver.getCursor(),
                              new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.DURATION},
                              new int[]{R.id.text1, R.id.text2, R.id.text3},
                              0);

    this.listView.setAdapter(adapter);
    this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("CallLogFragment", position + "");
      }
    });
  }

}
