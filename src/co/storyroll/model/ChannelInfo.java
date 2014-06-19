package co.storyroll.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
* Created by martynas on 18/06/14.
*/
public class ChannelInfo {
    private Channel channel;
    private int unseenCount = 0;
    private long lastActivityTime = 0;
    private List<String> userAvatars = new ArrayList<String>();


    public ChannelInfo(Channel channel, int unseenCount){
        this.channel = channel;
        this.unseenCount = unseenCount;
    }

    public ChannelInfo(JSONObject json) throws JSONException {
        if (json==null) return;
        this.channel = new Channel(json.getJSONObject("channel"));
        this.unseenCount = json.getInt("unseenCount");
        lastActivityTime = json.getLong("lastActivityTime");

        JSONArray users = json.getJSONArray("5Users");
        if (users!=null) {
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                userAvatars.add(user.getString("avatarUrl"));
            }
        }
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

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public List<String> getUserAvatars() {
        return userAvatars;
    }

    public void setUserAvatars(List<String> userAvatars) {
        this.userAvatars = userAvatars;
    }
}
