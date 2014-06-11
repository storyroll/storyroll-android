package co.storyroll.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import co.storyroll.R;

/**
 * Created by martynas on 11/06/14.
 */
public class FragmentTab1 extends Fragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        View v = inflater.inflate(R.layout.fragment_pager_list, container, false);
        View tv = v.findViewById(R.id.text);
        ((TextView)tv).setText("Fragment #" + 1);

        Button findBtn = (Button)v.findViewById(R.id.findFriendsBtn);
        findBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doUploadAddressBook();
            }
        });
        return v;
    }

    private void doUploadAddressBook() {
        Log.v("ddd", "upload");

    }

}