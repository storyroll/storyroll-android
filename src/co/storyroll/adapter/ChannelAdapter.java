package co.storyroll.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.activity.MainChannelsActivity;
import co.storyroll.model.Channel;
import co.storyroll.model.ChannelInfo;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by martynas on 17/06/14.
 */
public class ChannelAdapter extends ArrayAdapter<ChannelInfo> {

    private static final String LOGTAG = "ChannelAdapter";
    private ArrayList<ChannelInfo> mContacts;
    private PQuery aq;
    private static final boolean HIDE_AGE_AGO_POSTFIX = true;
    private Calendar c = Calendar.getInstance();

    public final static int[] castIds = {R.id.cast1, R.id.cast2, R.id.cast3, R.id.cast4, R.id.cast5};



    public ChannelAdapter(Context context, ArrayList<ChannelInfo> items) {
        super(context, android.R.id.list, items);
        this.aq = ((MainChannelsActivity)context).getAQuery();
        Log.v(LOGTAG, "initializing "+LOGTAG+" with items: "+items.size());
        this.mContacts = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = vi.inflate(R.layout.channel_item, null);
        }

        final ChannelInfo channelInfo = mContacts.get(position);
        if (channelInfo != null) {
            Channel channel = channelInfo.getChannel();
            aq.id(view.findViewById(R.id.name)).text(channel.getTitle());

            if (channelInfo.getLastActivityTime()>0)
            {
                String ageText = DateUtils.getRelativeTimeSpanString(
                        channelInfo.getLastActivityTime(), c.getTimeInMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
                if (HIDE_AGE_AGO_POSTFIX) {
                    ageText = ageText.replace(" ago", "");
                }
                aq.id(view.findViewById(R.id.age)).text(ageText);
            }
            // show user thumbs
            for ( int i=0;i< castIds.length && i<channelInfo.getUserAvatars().size(); i++ ) {
                int cid = castIds[i];
                aq.id(view.findViewById(cid)).visible().image(channelInfo.getUserAvatars().get(i), true, false, 0, R.drawable.def_contact);
            }

            ImageView thumb = (ImageView) view.findViewById(R.id.thumb);
            aq.id(thumb).image(channel.getThumbUrl(), true, false, 0, R.drawable.def_contact);
        }

        return view;
    }
}
