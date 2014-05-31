package com.storyroll.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martynas on 31/05/14.
 */
public class Invite {
    Channel channel;
    Profile invitingUser;
    Long id;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Profile getInvitingUser() {
        return invitingUser;
    }

    public void setInvitingUser(Profile invitingUser) {
        this.invitingUser = invitingUser;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Invite(JSONObject json) throws JSONException {
        if (json!=null) {
            channel = new Channel(json.getJSONObject("channel"));
            invitingUser = new Profile(json.getJSONObject("invitingUser"));
            id = new Long(json.getLong("id"));
        }
    }

    public static List<Invite> invites(JSONArray ja) throws JSONException {
        List<Invite> invites = new ArrayList<Invite>();
        if (ja!=null) {
            for(int i=0;i<ja.length();i++) {
                invites.add(new Invite(ja.getJSONObject(i)));
            }
        }
        return invites;
    }
}
