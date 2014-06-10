package co.storyroll.ui;

import android.app.Activity;
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
 * Created by martynas on 02/06/14.
 */
public class LeaveChanDialog extends DialogFragment {
    private static final String LOGTAG = "ChannelDialog";

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface LeaveChanDialogListener {
        public void onDialogLeaveChannelConfirm(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    LeaveChanDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (LeaveChanDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement LeaveChanDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_leave_channel)
                .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.v(LOGTAG, "YES clicked");
                        getGTracker().send(MapBuilder
                                        .createEvent("ui_action", "channel_dialog", "leave_yes", null)
                                        .build()
                        );
                        mListener.onDialogLeaveChannelConfirm(LeaveChanDialog.this);
                    }
                })
                .setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.v(LOGTAG, "NO clicked");
                        getGTracker().send(MapBuilder
                                        .createEvent("ui_action", "channel_dialog", "leave_no", null)
                                        .build()
                        );
                    }
                })
                .setTitle(R.string.dialog_signup_title);
        // Create the AlertDialog object and return it
        return builder.create();
    }

    protected EasyTracker getGTracker() {
        return EasyTracker.getInstance(getActivity());
    }

}