package com.hoccer.xo.android.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.provider.ContactsContract;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.util.DeviceContact;
import com.artcom.hoccer.R;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class DeviceContactsAdapter extends BaseAdapter {

    private static final Logger LOG = Logger.getLogger(DeviceContactsAdapter.class);

    private Activity mActivity;
    private List<DeviceContact> mContacts;
    private List<DeviceContact> mQueriedContacts;
    private String mQuery;
    private List<String> mSelectedData;

    private LayoutInflater mInflater = null;

    // Constructor expects an ordered list of device contacts
    public DeviceContactsAdapter(List<DeviceContact> items, Activity activity) {
        mContacts = items;
        mActivity = activity;

        mQueriedContacts = mContacts;

        mSelectedData = new ArrayList<String>();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            if (mInflater == null) {
                mInflater = LayoutInflater.from(parent.getContext());
            }
            convertView = mInflater.inflate(R.layout.item_dialog_multi_invitation, null);
        }

        DeviceContact contact = mQueriedContacts.get(position);
        TextView displayNameView = (TextView) convertView.findViewById(R.id.tv_displayname);

        if(mQuery == null) {
            displayNameView.setText(contact.getDisplayName());
        } else {
            displayNameView.setText(getHighlightedSearchResult(contact.getDisplayName()));
        }

        QuickContactBadge quickContact = (QuickContactBadge) convertView.findViewById(R.id.cb_quickcontact);
        quickContact.assignContactUri(ContactsContract.Contacts.getLookupUri(0, contact.getLookupKey()));

        Picasso.with(mActivity)
                .load(contact.getThumbnailUri())
                .placeholder(R.drawable.ic_contact_picture)
                .error(R.drawable.ic_contact_picture)
                .into(quickContact);

        TextView dataView = (TextView) convertView.findViewById(R.id.tv_detailed_info);
        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
        RelativeLayout clicker = (RelativeLayout) convertView.findViewById(R.id.clickablelayout);

        boolean isSelected = false;
        final String[] contactDataList = contact.getDataItem();
        if(contactDataList.length > 0) {
            if(contactDataList.length == 1) {
                final String data = contactDataList[0];
                dataView.setText(data);
                isSelected = mSelectedData.contains(data);

                clicker.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(mSelectedData.contains(data)) {
                            mSelectedData.remove(data);
                        } else {
                            mSelectedData.add(data);
                        }
                        notifyDataSetChanged();
                    }
                });
            } else {
                dataView.setText(R.string.invite_several_entries);

                // is any of the data items selected
                for(String contactData : contactDataList) {
                    if(mSelectedData.contains(contactData)) {
                        isSelected = true;
                        break;
                    }
                }

                clicker.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // create selection states array
                        boolean[] selectionStates = new boolean[contactDataList.length];
                        for(int i = 0; i < selectionStates.length; i++) {
                            selectionStates[i] = mSelectedData.contains(contactDataList[i]);
                        }
                        XoDialogs.showMultiChoiceDialog("DeviceContactDialog",
                                R.string.dialog_device_contact_data_selection,
                                contactDataList,
                                selectionStates,
                                mActivity,
                                new XoDialogs.OnMultiSelectionFinishedListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id, boolean[] newSelectionStates) {
                                        for(int i = 0; i < newSelectionStates.length; i++) {
                                            String data = contactDataList[i];
                                            boolean isSelected = mSelectedData.contains(data);
                                            if(isSelected) {
                                                if(!newSelectionStates[i]) {
                                                    mSelectedData.remove(data);
                                                }
                                            } else {
                                                if(newSelectionStates[i]) {
                                                    mSelectedData.add(data);
                                                }
                                            }
                                        }
                                        notifyDataSetChanged();
                                    }
                                });
                    }
                });
            }
            checkBox.setChecked(isSelected);
        } else {
            LOG.error("No phone numbers found in contact");
        }
        return convertView;
    }

    @Override
    public int getCount() {
        return mQueriedContacts.size();
    }

    @Override
    public Object getItem(int position) {
        return mQueriedContacts.get(position);
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public String[] getSelectedData() {
        return mSelectedData.toArray(new String[mSelectedData.size()]);
    }

    /* Sets the new query which is used to filter the contact list.
     * If query is null no filtering is applied.
     * If query is empty string everything is filtered.
     */
    public void setQuery(String query) {
        if(query != null) {
            mQuery = query.toLowerCase();
            mQueriedContacts = new ArrayList<DeviceContact>();

            if(!mQuery.isEmpty()) {
                for (DeviceContact contact : mContacts) {
                    if (contact.getDisplayName().toLowerCase().contains(mQuery)) {
                        mQueriedContacts.add(contact);
                    }
                }
            }
        } else {
            mQuery = null;
            mQueriedContacts = mContacts;
        }

        notifyDataSetChanged();
    }

    public String getQuery() {
        return mQuery;
    }

    private Spannable getHighlightedSearchResult(String text) {
        Spannable result = new SpannableString(text);
        String lowerCaseText = text.toLowerCase();

        // initialize the string as not highlighted
        result.setSpan(new ForegroundColorSpan(Color.GRAY), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int fromIndex = 0;
        int highlightStart = lowerCaseText.indexOf(mQuery, fromIndex);
        while(highlightStart >= 0) {
            int highlightEnd = highlightStart + mQuery.length();
            result.setSpan(new ForegroundColorSpan(Color.BLACK), highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            fromIndex = highlightEnd;
            highlightStart = lowerCaseText.indexOf(mQuery, fromIndex);
        }

        return result;
    }
}