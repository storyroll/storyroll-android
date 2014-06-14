package co.storyroll.tasks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import co.storyroll.R;
import co.storyroll.activity.LoadContactsListener;
import co.storyroll.activity.TabListFragment;
import co.storyroll.model.Contact;

import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * Created by martynas on 12/06/14.
 */
public class AsyncLoadContacts extends AsyncTask<Void, Void, Void> {
    private static final String LOGTAG = "AsyncLoadContacts";
    private int tabNum = 0;
    private LoadContactsListener lcListener;
    private Activity act;
    private ProgressBar progress = null;
    private ImageButton button = null;
    private View view;

    private LinkedHashMap<String, Contact> allContacts = new LinkedHashMap<String, Contact>();
//    public static ArrayList<Contact> phoneContacts = new ArrayList<Contact>();



    public AsyncLoadContacts(int tabNum, LoadContactsListener lcl, Activity act, View v){
        super();
        this.tabNum = tabNum;
        this.lcListener = lcl;
        this.act = act;
        if (v!=null) {
            this.progress = (ProgressBar) v.findViewById(R.id.progress);
            this.button = (ImageButton) v.findViewById(R.id.findFriendsBtn);
            this.view = v;
        }
    }

    @Override
    protected void onPreExecute() {

//			progressLayout.setVisibility(View.VISIBLE);
        if (progress!=null) {
            progress.setVisibility(View.VISIBLE);
            button.setVisibility(View.GONE);
        }

    }

    @Override
    protected Void doInBackground(Void... params) {

        // Obtain contacts
        getContacts(this);
        return null;

    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        if (!isCancelled())
        {
            // set contact adapter
//                lv.setAdapter(contactAdapter);
            // upload

        }
        // set the progress to GONE
//			progressLayout.setVisibility(View.GONE);
        if (progress!=null) {
            progress.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
        }
        lcListener.onContactsLoaded(tabNum, view);

    }

    @Override
    protected void onCancelled() {
        Log.v(LOGTAG, "onCancelled");
        super.onCancelled();
    }

    @SuppressLint("InlinedApi")
    private void getContacts(AsyncTask task) {

        ContentResolver cr = act.getContentResolver();

//		Cursor cur = cr.query(Data.CONTENT_URI, new String[] { Data.CONTACT_ID, Data.MIMETYPE, Email.ADDRESS,
//				Contacts.DISPLAY_NAME, Phone.NUMBER }, null, null, Contacts.DISPLAY_NAME);
        Cursor cur = cr.query(ContactsContract.Data.CONTENT_URI, new String[] { ContactsContract.Data.CONTACT_ID, ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.Contacts.DISPLAY_NAME }, null, null, ContactsContract.Contacts.DISPLAY_NAME);
        Contact contact;

        if (cur.getCount() > 0) {

            while (cur.moveToNext()
//                    && !task.isCancelled()
                    ) {

                String id = cur.getString(cur.getColumnIndex(ContactsContract.Data.CONTACT_ID));

                String mimeType = cur.getString(cur.getColumnIndex(ContactsContract.Data.MIMETYPE));

                if (allContacts.containsKey(id)) {
                    // update contact
                    contact = allContacts.get(id);
                } else {
                    contact = new Contact();
                    // only add if it's not a phone-only contact
                    allContacts.put(id, contact);
                    // set photoUri
                    contact.setContactPhotoUri(getContactPhotoUri(Long.parseLong(id)));
                }

                if (mimeType.equals(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE))
                    // set name
                    contact.setContactName(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));

//				if (mimeType.equals(Phone.CONTENT_ITEM_TYPE))
//					// set phone munber
//					contact.setContactNumber(cur.getString(cur.getColumnIndex(Phone.NUMBER)));

                if (mimeType.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE))
                    // set email
                    contact.setContactEmail(cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)));

            }
        }

        cur.close();

        if (true
//            && !task.isCancelled()
                ) {
            // get contacts from hashmap
            TabListFragment.phoneContacts.clear();
            TabListFragment.phoneContacts.addAll(allContacts.values());

            for (Contact _contact : allContacts.values()) {

                // remove self contact
                if (_contact.getContactName() == null && _contact.getContactNumber() == null
                        && _contact.getContactEmail() == null) {
                    TabListFragment.phoneContacts.remove(_contact);
                    break;
                } else
                    // remove non-email or unnamed contacts
                    if (TextUtils.isEmpty(_contact.getContactName())
                            || TextUtils.isEmpty(_contact.getContactEmail())
                            || _contact.getContactName().equals(_contact.getContactEmail())) {
                        TabListFragment.phoneContacts.remove(_contact);
                    }

            }
            Collections.sort(TabListFragment.phoneContacts);
        }
    }


    // Get contact photo URI for contactId
    public Uri getContactPhotoUri(long contactId) {
        Uri photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        photoUri = Uri.withAppendedPath(photoUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        return photoUri;
    }

}
