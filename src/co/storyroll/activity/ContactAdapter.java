package co.storyroll.activity;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.storyroll.R;
import co.storyroll.model.Contact;

import java.util.*;

/**
 * Created by martynas on 12/06/14.
 */

// Contact adapter
public class ContactAdapter extends ArrayAdapter<Contact> implements SectionIndexer {

    private static final String LOGTAG = "ContactAdapter";
    private ArrayList<Contact> contactList;
    public ArrayList<Contact> originalList;
    private ContactFilter filter;
    private HashMap<String, Integer> alphaIndexer;
    private String[] sections;
    public Set<String> selected = new HashSet<String>();


    public ContactAdapter(Context context, ArrayList<Contact> items) {
        super(context, android.R.id.list, items);
        Log.v(LOGTAG, "initializing ContactAdapter with items: "+items.size());
        this.contactList = items;
//            this.contactList = new ArrayList<Contact>();
        this.originalList = new ArrayList<Contact>();

        this.contactList.addAll(items);
        this.originalList.addAll(items);

        // indexing
        alphaIndexer = new HashMap<String, Integer>();

        int size = contactList.size();

        for (int x = 0; x < size; x++) {
            String s = contactList.get(x).getContactName();
            if (s==null) {
                s=" ";
            }
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
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = vi.inflate(R.layout.contact_item, null);
        }

        final Contact contact = contactList.get(position);
        if (contact != null) {
            TextView name = (TextView) view.findViewById(R.id.name);
            ImageView thumb = (ImageView) view.findViewById(R.id.thumb);
            final TextView number = (TextView) view.findViewById(R.id.number);
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

            nameCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if ((nameCheckBox.isChecked() && numSelected>=AddressTabsActivity.MAX_INVITES_ALLOWED))
                    {
                        Toast.makeText(getContext(), R.string.contacts_too_many, Toast.LENGTH_SHORT).show();
                        contact.setSelected(false);
                        nameCheckBox.setChecked(false);
                    }
                    else {
                        numSelected += (nameCheckBox.isChecked() ? 1 : -1);
                        contact.setSelected(nameCheckBox.isChecked());
                        Log.v(LOGTAG, "numSelected: " + numSelected);

                        if (nameCheckBox.isChecked()) {
                            selected.add(contact.getContactEmail());
                        }
                        else {
                            selected.remove(contact.getContactEmail());
                        }
                    }
                }
            });

            nameCheckBox.setChecked(contact.isSelected());
        }

        return view;
    }

    private int numSelected = 0;

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