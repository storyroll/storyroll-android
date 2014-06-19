package co.storyroll.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import co.storyroll.R;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

/**
 * Created by martynas on 05/06/14.
 */
public class HideMovieDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_remove_movie)
                .setPositiveButton(R.string.btn_not_interested, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        getGTracker().send(MapBuilder
                                        .createEvent("ui_action", "touch", "hide_movie_not_interested", null)
                                        .build()
                        );
                    }
                })
                .setNegativeButton(R.string.btn_interested, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getGTracker().send(MapBuilder
                                .createEvent("ui_action", "touch", "hide_movie_not_NOT_interested", null)
                                .build()
                        );
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    protected EasyTracker getGTracker() {
        return EasyTracker.getInstance(getActivity());
    }
}
