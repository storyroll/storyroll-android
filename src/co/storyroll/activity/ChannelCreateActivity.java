package co.storyroll.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import co.storyroll.R;
import co.storyroll.base.MenuActivity;
import co.storyroll.util.AppUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.Fields;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by martynas on 28/05/14.
 */
public class ChannelCreateActivity extends MenuActivity {
    private final static String SCREEN_NAME = "Channel";
    protected static final String LOGTAG = "ChannelCreate";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isHomeEnabled = !getIntent().getBooleanExtra("logout", false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_channel);

        aq.id(R.id.done_button).enabled(true).clicked(this, "doneButtonClicked");

        // Fields set on a tracker persist for all hits, until they are
        // overridden or cleared by assignment to null.
        getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);

    }

    public void doneButtonClicked(View button)
    {
        aq.id(R.id.done_button).enabled(false);

        String chName = aq.id(R.id.channel_name).getText().toString();
        if (TextUtils.isEmpty(chName)) {
            Toast.makeText(aq.getContext(), R.string.msg_channel_name_required, Toast.LENGTH_SHORT).show();
            aq.id(R.id.done_button).enabled(true);
            return;
        }
        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_CHANNEL_CREATE, "uuid=" + getUuid() + "&t=" + chName);
        aq.auth(basicHandle).progress(R.id.progress_circular).ajax(apiUrl, JSONObject.class, this, "channelCreateCb");
    }

    public void channelCreateCb(String url, JSONObject json, AjaxStatus status)
    {
        Log.v(LOGTAG, "channelCreateCb");
        // profile register successfull or fail?
        fireGAnalyticsEvent("channel", "create", json != null?"success":"fail", null);

        channelUpdateGeneral(url, json, status);
    }

    public void channelUpdateCb(String url, JSONObject json, AjaxStatus status)
    {
        Log.v(LOGTAG, "channelUpdateCb");
        fireGAnalyticsEvent("channel", "update", json != null?"success":"fail", null);
        channelUpdateGeneral(url, json, status);
    }

    private Long chanId = null;

    protected boolean channelUpdateGeneral(String url, JSONObject json, AjaxStatus status)
    {
        if (status.getCode()==500)
        {
            if (status.getError().contains("already present")) {
                Log.w(LOGTAG, "Channel already present");
                Toast.makeText(this, R.string.msg_uname_not_unique, Toast.LENGTH_SHORT).show();
                aq.id(R.id.done_button).enabled(true);
                return false;
            }
        }
        if (isAjaxErrorThenReport(status)) {
            aq.id(R.id.done_button).enabled(true);
            return false;
        }

        if(json != null){
            //successful ajax call
            try {
                chanId = json.getLong("id");
                Log.v(LOGTAG, "channelUpdateGeneral success: "+chanId);

            } catch (JSONException e) {
                e.printStackTrace();
                apiError(LOGTAG, "JSON Parse error: "+e.getMessage(), status, false, Log.ERROR);
                Toast.makeText(this, "Unexpected error creating channel", Toast.LENGTH_SHORT).show();
            }
            returnHomeActivity();

        }else{
            apiError(LOGTAG, "Could not update channel", status, true, Log.ERROR);
            aq.id(R.id.done_button).enabled(true);
            return false;
        }
        return true;
    }

    @Override
    protected void returnHomeActivity() {
        Intent intent = new Intent(this, AppUtility.ACTIVITY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(ChannelActivity.EXTRA_CHANNEL_ID, chanId);
        aq.id(R.id.done_button).enabled(true);
        startActivity(intent);
    }

}
