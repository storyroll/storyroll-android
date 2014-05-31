package com.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Profile implements Serializable {
	
	public static final int AVATAR_WIDTH = 200;
	public static final int AVATAR_HEIGHT = 200;
	
	public static final Integer AUTH_UNKNOWN = 0;
	public static final Integer AUTH_EMAIL = 1;
	public static final Integer AUTH_FACEBOOK = 2;
	public static final Integer AUTH_TWITTER = 3;
	public static final Integer AUTH_GPLUS = 4;
	
	public String id = null;
    public String username = null;
    public String password = null;
    public String email = null;
    public String birthday = null;
	public String gender = null;
	public String gcmRegistrationId = null;
	
    public String avatarUrl = null;
    public Boolean loggedIn = false;
    public Integer authMethod = AUTH_UNKNOWN;
    public String location = null;
    
    public Profile() {
    	
    }
    
    public Profile(String id, String username, String password, String location, Integer authMethod) {
    	this.id = id;
    	this.username = username;
    	this.password = password;
    	this.location = location;
    	this.authMethod = authMethod;
    }

    public Profile(JSONObject json) throws JSONException {
        if (json!=null) {
            avatarUrl = json.getString("avatarUrl");
            email = json.getString("email");
            username = json.getString("username");
        }
    }
    
    public boolean isAuthFacebook() {
    	return AUTH_FACEBOOK.equals(authMethod);
    }
    
    public boolean isAuthEmail() {
    	return AUTH_EMAIL.equals(authMethod);
    }
    
    public String toParamString(boolean addUsername, boolean forceAllFields) {
    	String s = "uuid="+email;
    	if (addUsername && username!=null) {
    		s+="&username="+username;
    	} else if (forceAllFields) {
    		s+="&username="+(username==null?"":username);
    	};
    	
    	if (location!=null) {
    		s+="&location="+location;
    	}
    	else if (forceAllFields) {
    		s+="&location="+(location==null?"":location);
    	};
    	
    	if (forceAllFields) {
    		s+="&auth_method="+(authMethod==null?AUTH_UNKNOWN:authMethod);
    		s+="&password="+(password==null?"":password);
    		s+="&regid="+(gcmRegistrationId==null?"":gcmRegistrationId);
    	}
    	return s;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
	public String toString() {
		return "Profile [id=" + id + ", username=" + username + ", password="
				+ password + ", email=" + email + ", birthday=" + birthday
				+ ", gender=" + gender + ", gcmRegistrationId="
				+ gcmRegistrationId + ", avatarUrl=" + avatarUrl + ", loggedIn="
				+ loggedIn + ", authMethod=" + authMethod + ", location="
				+ location + "]";
	}


}
