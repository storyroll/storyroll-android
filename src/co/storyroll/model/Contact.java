package co.storyroll.model;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by martynas on 28/05/14.
 */

public class Contact implements Parcelable, Comparable<Contact> {

    private String contactName;
    private String contactNumber;
    private Bitmap contactPhoto;
    private Uri contactPhotoUri;
    private String contactEmail;
    private String contactPhotoUrl;

    boolean selected = false;

    public Contact() {
        ;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public Bitmap getContactPhoto() {
        return contactPhoto;
    }

    public Uri getContactPhotoUri() {
        return contactPhotoUri;
    }

    public String getContactPhotoUrl() { return contactPhotoUrl; }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setContactPhoto(Bitmap contactPhoto) {
        this.contactPhoto = contactPhoto;

    }

    public void setContactPhotoUri(Uri contactPhotoUri) {
        this.contactPhotoUri = contactPhotoUri;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return contactName + " " + contactEmail + " ";
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(contactName);
        dest.writeString(contactNumber);
        dest.writeString(contactPhotoUri.toString());
        dest.writeString(contactEmail);

    }

    public Contact(Parcel source) {

        contactName = source.readString();
        contactNumber = source.readString();
        contactPhotoUri = Uri.parse(source.readString());
        contactPhotoUri = Uri.withAppendedPath(contactPhotoUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        contactEmail = source.readString();
    }

    public Contact(JSONObject profileJson) throws JSONException {
        if (profileJson!=null) {
            contactPhotoUri = null;
            contactPhotoUrl = profileJson.getString("avatarUrl");
            contactEmail = profileJson.getString("email");
            contactName = profileJson.getString("username");
        }
    }

    public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {

        @Override
        public Contact createFromParcel(Parcel source) {
            return new Contact(source);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }

    };

    @Override
    public int compareTo(Contact contact) {
        return getContactName().compareToIgnoreCase(contact.getContactName());
    }
}
