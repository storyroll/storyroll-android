package com.storyroll.model;

public class Profile {
	
	public static final Integer AUTH_UNKNOWN = 0;
	public static final Integer AUTH_EMAIL = 1;
	public static final Integer AUTH_FACEBOOK = 2;
	public static final Integer AUTH_TWITTER = 3;
	public static final Integer AUTH_GPLUS = 4;
	
	public String id = null;
    public String username = null;
    public String email = null;
    public Integer avatar = null;
    public Boolean loggedIn = false;
    public Integer authMethod = AUTH_UNKNOWN;
    public String location = null;
    
    public boolean isAuthFacebook() {
    	return authMethod == AUTH_FACEBOOK;
    }
    
	@Override
	public String toString() {
		return "Profile [id=" + id + ", name=" + username + ", email=" + email
				+ ", avatar=" + avatar + ", location=" + location + ", loggedIn=" + loggedIn
				+ ", `auth=" + authMethod + "]";
	}


}
