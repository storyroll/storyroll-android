package co.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by martynas on 20/06/14.
 */
public class Cast {
    private String avatarUrl;
    private String username;
    private String location;
    private String uuid;

    public Cast(JSONObject owner) throws JSONException {
        avatarUrl = owner.getString("avatarUrl");
        username = owner.getString("username");
        location = owner.getString("location");
        uuid = owner.getString("uuid");
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
