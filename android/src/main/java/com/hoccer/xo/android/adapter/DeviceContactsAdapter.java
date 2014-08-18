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

//    @Override
//    public void bindView(View view, Context context, Cursor cursor) {
//        final ViewHolder holder = (ViewHolder) view.getTag();
//        final String photoData = cursor.getString(ContactsQuery.THUMBNAIL_URI_FIELD);
//        final String displayName = cursor.getString(ContactsQuery.DISPLAY_NAME_FIELD);
//        String info = "";
//        String detailSelection = "";
//        if (mIsSmsInvitation) {
//            info =  cursor.getString(ContactsQuery.PHONE_NUMBER_FIELD);
//            detailSelection = Contacts.LOOKUP_KEY + "=?" + " AND " + ContactsContract.Data.MIMETYPE  + "='" +
//                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
//        } else {
//            info = cursor.getString(ContactsQuery.EMAIL_ADDRESS_FIELD);
//            detailSelection = Contacts.LOOKUP_KEY + "=?" + " AND " + ContactsContract.Data.MIMETYPE  + "='" +
//                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'";
//        }
//        final String detailedInfo = info;
//        final Uri contactUri = Contacts.getLookupUri(
//                cursor.getLong(ContactsQuery.ID),
//                cursor.getString(ContactsQuery.LOOKUP_KEY_FIELD));
//        holder.quickContact.assignContactUri(contactUri);
//        final Bitmap thumbnailBitmap =  loadContactPhotoThumbnail(photoData);
//        if (thumbnailBitmap != null) {
//            holder.quickContact.setImageBitmap(thumbnailBitmap);
//        } else {
//            holder.quickContact.setImageToDefault();
//        }
//        final String selection = detailSelection;
//        final String[] id = new String[]{cursor.getString(ContactsQuery.LOOKUP_KEY_FIELD)};
//        Cursor individualContactCursor = mContext.getContentResolver().query(ContactsQuery.CONTENT_URI,
//                ContactsQuery.PROJECTION, selection, id, null);
//        individualContactCursor.moveToFirst();
//            holder.detailedInfo.setText(R.string.invite_several_entries);
//        if (individualContactCursor.getCount() > 1) {
//            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
//                    holder.checkBox.setChecked(!checked);
//                    if (holder.detailedInfo.getText().toString().
//                            equals(mContext.getResources().getString(R.string.invite_several_entries))) {
//                        holder.checkBox.setChecked(false);
//                    } else {
//                        holder.checkBox.setChecked(true);
//                    }
//                    if (!mOnCheckedCalledFromOnChecked) {
//                        final DetailedPhoneBookDialog d = new DetailedPhoneBookDialog(mContext, selection, id, thumbnailBitmap, mRecipientsSet);
//                        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                            @Override
//                            public void onDismiss(DialogInterface dialogInterface) {
//                                if (d.getDeselectedNumbers().isEmpty() && d.getSelectedNumbers().isEmpty()) {
//                                    mOnCheckedCalledFromOnChecked = false;
//                                    return;
//                                }
//                                mOnCheckedCalledFromOnChecked = true;
//                                mRecipientsSet.removeAll(d.getDeselectedNumbers());
//                                mRecipientsSet.addAll(d.getSelectedNumbers());
//                                String info = "";
//                                for (String number: d.getSelectedNumbers()) {
//                                    info += number + ", ";
//                                }
//                                if (info.isEmpty()) {
//                                    if (!holder.checkBox.isChecked()) {
//                                        mOnCheckedCalledFromOnChecked = false;
//                                    }
//                                    holder.detailedInfo.setText(R.string.invite_several_entries);
//                                    holder.checkBox.setChecked(false);
//                                } else {
//                                    info = info.substring(0, info.lastIndexOf(","));
//                                    holder.detailedInfo.setText(info);
//                                    if (holder.checkBox.isChecked()) {
//                                        mOnCheckedCalledFromOnChecked = false;
//                                    }
//                                    holder.checkBox.setChecked(true);
//                                }
//                                d.closeCursor();
//                            }
//                        });
//                        d.show();
//                    } else {
//                        mOnCheckedCalledFromOnChecked = false;
//                    }
//                }
//            });
//        } else {
//            holder.detailedInfo.setText(detailedInfo);
//            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
//                    if (checked) {
//                        mRecipientsSet.add(detailedInfo);
//                    } else {
//                        mRecipientsSet.remove(detailedInfo);
//                    }
//                }
//            });
//        }
//        individualContactCursor.close();
//        if (mRecipientsSet.contains(detailedInfo)) {
//            holder.checkBox.setChecked(true);
//        } else {
//            holder.checkBox.setChecked(false);
//        }
//        holder.displayName.setText(displayName);
//        holder.clicker.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                holder.checkBox.setChecked(!holder.checkBox.isChecked());
//            }
//        });
//    }
//
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