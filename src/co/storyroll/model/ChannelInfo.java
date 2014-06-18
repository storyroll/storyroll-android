package co.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
* Created by martynas on 18/06/14.
*/
public class ChannelInfo {
    private Channel channel;
    private int unseenCount = 0;

    public ChannelInfo(Channel channel, int unseenCount){
        this.channel = channel;
        this.unseenCount = unseenCount;
    }

    public ChannelInfo(JSONObject json) throws JSONException {
        if (json==null) return;
        this.channel = new Channel(json.getJSONObject("channel"));
        this.unseenCount = json.getInt("unseenCount");
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public int getUnseenCount() {
        return unseenCount;
    }

    public void setUnseenCount(int unseenCount) {
        this.unseenCount = unseenCount;
    }
}
