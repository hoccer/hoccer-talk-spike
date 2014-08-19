package com.hoccer.xo.android.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.util.DeviceContact;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class DeviceContactsAdapter extends BaseAdapter {

    private static final Logger LOG = Logger.getLogger(DeviceContactsAdapter.class);

    public enum DataType {
        PhoneNumber,
        EMailAddress
    }

    private Activity mActivity;
    private List<DeviceContact> mContacts;
    private List<String> mSelectedData;
    private DataType mDataType;
    private LayoutInflater mInflater = null;

    // Constructor expects an ordered list of device contacts and the data type to show
    public DeviceContactsAdapter(List<DeviceContact> items, DataType dataType, Activity activity) {
        mContacts = items;
        mDataType = dataType;
        mActivity = activity;

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

        DeviceContact contact = mContacts.get(position);
        TextView displayNameView = (TextView) convertView.findViewById(R.id.tv_displayname);
        displayNameView.setText(contact.getDisplayName());

        QuickContactBadge quickContact = (QuickContactBadge) convertView.findViewById(R.id.cb_quickcontact);
        quickContact.assignContactUri(ContactsContract.Contacts.getLookupUri(0, contact.getLookupKey()));
        Bitmap thumbnailBitmap =  loadContactPhotoThumbnail(contact.getThumbnailUri());
        if (thumbnailBitmap != null) {
            quickContact.setImageBitmap(thumbnailBitmap);
        } else {
            quickContact.setImageToDefault();
        }

        TextView dataView = (TextView) convertView.findViewById(R.id.tv_detailed_info);
        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
        RelativeLayout clicker = (RelativeLayout) convertView.findViewById(R.id.clickablelayout);

        boolean isSelected = false;
        final String[] contactDataList = getContactData(contact);
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
        return mContacts.size();
    }

    @Override
    public Object getItem(int position) {
        return mContacts.get(position);
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

    private String[] getContactData(DeviceContact contact) {
        switch(mDataType) {
            case PhoneNumber:
                return contact.getPhoneNumbers();
            case EMailAddress:
                return contact.getEMailAddresses();
            default:
                throw new IllegalArgumentException("Invalid data type encountered");
        }
    }

    private Bitmap loadContactPhotoThumbnail(String photoData) {
        if (photoData == null) {
            return null;
        }
        AssetFileDescriptor afd = null;
        try {
            Uri thumbUri = Uri.parse(photoData);
            afd = mActivity.getContentResolver().openAssetFileDescriptor(thumbUri, "r");
            FileDescriptor fileDescriptor = afd.getFileDescriptor();
            if (fileDescriptor != null) {
                return BitmapFactory.decodeFileDescriptor(fileDescriptor);
            }
        } catch (FileNotFoundException e) {

        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}