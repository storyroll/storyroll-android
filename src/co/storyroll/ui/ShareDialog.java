package co.storyroll.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import co.storyroll.R;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

/**
 * Created by martynas on 09/06/14.
 */
public class ShareDialog extends DialogFragment {
    private static final String LOGTAG = "SHARE";


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.pick_answer_share)
                .setItems(R.array.share_scene_values, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        String selection = getActivity().getResources().getStringArray(R.array.share_scene_values)[which];
                        Log.v(LOGTAG, "selected: "+selection);
                        getGTracker().send(MapBuilder
                                        .createEvent("ui_action", "share_dialog", selection, null)
                                        .build()
                        );
                    }
                });
        return builder.create();
    }

    protected EasyTracker getGTracker() {
        return EasyTracker.getInstance(getActivity());
    }

}
