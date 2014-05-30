package com.storyroll.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.View.OnTouchListener;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.storyroll.R;
import com.storyroll.model.Contact;
import com.storyroll.util.ContactUtil;

import java.util.*;

//import android.provider.ContactsContract.CommonDataKinds.Phone;

public final class ContactManager extends Activity {

    private static final String LOGTAG = "ContactManager";
    private static ArrayList<Contact> contacts = null;
	private LinkedHashMap<String, Contact> allContacts = new LinkedHashMap<String, Contact>();
	private ContactAdapter contactAdapter = null;
	private ListView lv;
	ImageView doneSelect;
	RelativeLayout progressLayout;
	EditText myFilter;

	// Indexing fo the list
	HashMap<String, Integer> alphaIndexer;
	String[] sections;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// remove title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.contact_manager);

		// /////////// Custom progress Layout //////////////////////
		progressLayout = (RelativeLayout) findViewById(R.id.progress_layout);

		progressLayout.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});
		progressLayout.setVisibility(View.GONE); // by default progress view to GONE
		// /////////////////////////////////////

		// Init UI elements
		lv = (ListView) findViewById(R.id.contactList);
		doneSelect = (ImageView) findViewById(R.id.doneSelect);
		myFilter = (EditText) findViewById(R.id.search_txt);

		// Register handler for UI elements
		doneSelect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                Log.v(LOGTAG, "doneSelect.onClick");
				ContactUtil.hideSoftKeyboard(ContactManager.this);
				setSelctedContacts();
			}

		});

		// Add text listener to the edit text for filtering the List
		myFilter.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// call the filter with the current text on the editbox
				contactAdapter.getFilter().filter(s.toString());
			}
		});

		if (contacts == null) {
			contacts = new ArrayList<Contact>();
			// Asynchronously load all contacts
			new AsyncLoadContacts().execute();
		} else {
			contactAdapter = new ContactAdapter(this, R.id.contactList, contacts);
			lv.setAdapter(contactAdapter);

		}

	}

	// set selected contacts on DONE button press
	private void setSelctedContacts() {
        Log.v(LOGTAG, "setSelctedContacts");
		ArrayList<Contact> selectedList = new ArrayList<Contact>();

		Intent intent = new Intent();

		ArrayList<Contact> contactList = contactAdapter.originalList;
		for (int i = 0; i < contactList.size(); i++) {
			Contact contact = contactList.get(i);
			if (contact.isSelected()) {
				selectedList.add(contact);
			}
			if (selectedList.size() > 0) {
				intent.putParcelableArrayListExtra("SELECTED_CONTACTS", selectedList);
				setResult(RESULT_OK, intent);
			} else {
				setResult(RESULT_CANCELED, intent);
			}
		}
		// Tip: Here you can finish this activity and on the Activty result of the calling activity you fetch the Selected contacts
        finish();
	}

	// Also on back pressed set the selected list, if nothing selected set Intent result to canceled
	@Override
	public void onBackPressed() {

		ArrayList<Contact> selectedList = new ArrayList<Contact>();

		Intent intent = new Intent();

		ArrayList<Contact> contactList = contactAdapter.originalList;
		for (int i = 0; i < contactList.size(); i++) {
			Contact contact = contactList.get(i);
			if (contact.isSelected()) {
				selectedList.add(contact);
			}
			if (selectedList.size() > 0) {
				intent.putParcelableArrayListExtra("SELECTED_CONTACTS", selectedList);
				setResult(RESULT_OK, intent);
			} else {
				setResult(RESULT_CANCELED, intent);
			}
		}

		finish();

	};

	@SuppressLint("InlinedApi")
	private void getContacts() {

		ContentResolver cr = getContentResolver();

//		Cursor cur = cr.query(Data.CONTENT_URI, new String[] { Data.CONTACT_ID, Data.MIMETYPE, Email.ADDRESS,
//				Contacts.DISPLAY_NAME, Phone.NUMBER }, null, null, Contacts.DISPLAY_NAME);
        Cursor cur = cr.query(Data.CONTENT_URI, new String[] { Data.CONTACT_ID, Data.MIMETYPE, Email.ADDRESS,
                Contacts.DISPLAY_NAME }, null, null, Contacts.DISPLAY_NAME);
		Contact contact;

		if (cur.getCount() > 0) {

			while (cur.moveToNext()) {

				String id = cur.getString(cur.getColumnIndex(Data.CONTACT_ID));

				String mimeType = cur.getString(cur.getColumnIndex(Data.MIMETYPE));

				if (allContacts.containsKey(id)) {
					// update contact
					contact = allContacts.get(id);
				} else {
					contact = new Contact();
                    // only add if it's not a phone contact only
                    allContacts.put(id, contact);
                    // set photoUri
                    contact.setContactPhotoUri(getContactPhotoUri(Long.parseLong(id)));
				}

				if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE))
					// set name
					contact.setContactName(cur.getString(cur.getColumnIndex(Contacts.DISPLAY_NAME)));

//				if (mimeType.equals(Phone.CONTENT_ITEM_TYPE))
//					// set phone munber
//					contact.setContactNumber(cur.getString(cur.getColumnIndex(Phone.NUMBER)));

				if (mimeType.equals(Email.CONTENT_ITEM_TYPE))
					// set email
					contact.setContactEmail(cur.getString(cur.getColumnIndex(Email.ADDRESS)));

			}
		}

		cur.close();
		// get contacts from hashmap
		contacts.clear();
		contacts.addAll(allContacts.values());

		for (Contact _contact : allContacts.values()) {

            // remove self contact
			if (_contact.getContactName() == null && _contact.getContactNumber() == null
					&& _contact.getContactEmail() == null) {
				contacts.remove(_contact);
				break;
			}
            else
                // remove non-email or unnamed contacts
                if (TextUtils.isEmpty(_contact.getContactName())
                        || TextUtils.isEmpty(_contact.getContactEmail())
                        || _contact.getContactName().equals(_contact.getContactEmail())) {
                contacts.remove(_contact);
            }

		}

		contactAdapter = new ContactAdapter(this, R.id.contactList, contacts);
		contactAdapter.notifyDataSetChanged();

	}

	// Get contact photo URI for contactId
	public Uri getContactPhotoUri(long contactId) {
		Uri photoUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
		photoUri = Uri.withAppendedPath(photoUri, Contacts.Photo.CONTENT_DIRECTORY);
		return photoUri;
	}

	private class AsyncLoadContacts extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {

			progressLayout.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params) {

			// Obtain contacts
			getContacts();
			return null;

		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			// set contact adapter
			lv.setAdapter(contactAdapter);

			// set the progress to GONE
			progressLayout.setVisibility(View.GONE);
		}

	}

	// Contact adapter
	public class ContactAdapter extends ArrayAdapter<Contact> implements SectionIndexer {

		private ArrayList<Contact> contactList;
		private ArrayList<Contact> originalList;
		private ContactFilter filter;

		public ContactAdapter(Context context, int textViewResourceId, ArrayList<Contact> items) {
			super(context, textViewResourceId, items);

			this.contactList = new ArrayList<Contact>();
			this.originalList = new ArrayList<Contact>();

			this.contactList.addAll(items);
			this.originalList.addAll(items);

			// indexing
			alphaIndexer = new HashMap<String, Integer>();
			int size = contactList.size();

			for (int x = 0; x < size; x++) {
				String s = contactList.get(x).getContactName();

				// get the first letter of the store
				String ch = s.substring(0, 1);
				// convert to uppercase otherwise lowercase a -z will be sorted
				// after upper A-Z
				ch = ch.toUpperCase();

				// HashMap will prevent duplicates
				alphaIndexer.put(ch, x);
			}

			Set<String> sectionLetters = alphaIndexer.keySet();

			// create a list from the set to sort
			ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);

			Collections.sort(sectionList);

			sections = new String[sectionList.size()];

			sectionList.toArray(sections);
		}

		@Override
		public Filter getFilter() {
			if (filter == null) {
				filter = new ContactFilter();
			}
			return filter;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			if (view == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.contact_item, null);
			}
			final Contact contact = contactList.get(position);
			if (contact != null) {
				TextView name = (TextView) view.findViewById(R.id.name);
				ImageView thumb = (ImageView) view.findViewById(R.id.thumb);
				TextView number = (TextView) view.findViewById(R.id.number);
				TextView email = (TextView) view.findViewById(R.id.email);

				// labels
				TextView numberLabel = (TextView) view.findViewById(R.id.numberLabel);
				TextView emailLabel = (TextView) view.findViewById(R.id.emailLabel);

                thumb.setImageURI(contact.getContactPhotoUri());

				if (thumb.getDrawable() == null)
					thumb.setImageResource(R.drawable.def_contact);

				final CheckBox nameCheckBox = (CheckBox) view.findViewById(R.id.checkBox);

				name.setText(contact.getContactName());

				// set number label
				if (contact.getContactNumber() == null)
					numberLabel.setText("");
				else
					numberLabel.setText("P: ");

				number.setText(contact.getContactNumber());

				if (contact.getContactEmail() == null)
					emailLabel.setText("");
				else
					emailLabel.setText("E: ");

				email.setText(contact.getContactEmail());

				nameCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

						contact.setSelected(nameCheckBox.isChecked());
					}
				});

				nameCheckBox.setChecked(contact.isSelected());
			}

			return view;
		}

		@Override
		public int getPositionForSection(int section) {
			return alphaIndexer.get(sections[section]);
		}

		@Override
		public int getSectionForPosition(int position) {
			return 0;
		}

		@Override
		public Object[] getSections() {
			return sections;
		}

		// Contacts filter
		private class ContactFilter extends Filter {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {

				constraint = constraint.toString().toLowerCase();
				FilterResults result = new FilterResults();
				if (constraint != null && constraint.toString().length() > 0) {
					ArrayList<Contact> filteredItems = new ArrayList<Contact>();

					for (int i = 0, l = originalList.size(); i < l; i++) {
						Contact contact = originalList.get(i);
						if (contact.toString().toLowerCase().contains(constraint))
							filteredItems.add(contact);
					}
					result.count = filteredItems.size();
					result.values = filteredItems;
				} else {
					synchronized (this) {
						result.values = originalList;
						result.count = originalList.size();
					}
				}
				return result;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {

				contactList = (ArrayList<Contact>) results.values;
				notifyDataSetChanged();
				clear();
				for (int i = 0, l = contactList.size(); i < l; i++)
					add(contactList.get(i));
				notifyDataSetInvalidated();
			}
		}

	}

}