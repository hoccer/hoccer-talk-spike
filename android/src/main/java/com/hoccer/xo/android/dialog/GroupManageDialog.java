package com.hoccer.xo.android.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.adapter.ContactsAdapter;
import com.hoccer.xo.android.adapter.GroupManagementContactsAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.fragment.GroupProfileCreationFragment;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class GroupManageDialog extends DialogFragment {

    private static final Logger LOG = Logger.getLogger(GroupManageDialog.class);

    private TalkClientContact mGroup;

    private ContactsAdapter mAdapter;
    private ArrayList<TalkClientContact> mContactsToInvite;
    private ArrayList<TalkClientContact> mContactsToKick;
    private ArrayList<TalkClientContact> mCurrentContactsInGroup = new ArrayList<TalkClientContact>();

    public GroupManageDialog(TalkClientContact group, ArrayList<TalkClientContact> currentContactsInGroup) {
        super();
        mGroup = group;
        mContactsToInvite = new ArrayList();
        mContactsToKick = new ArrayList();
        mCurrentContactsInGroup.addAll(currentContactsInGroup);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LOG.debug("onCreateDialog()");
        if(mAdapter == null) {
            mAdapter = new GroupManagementContactsAdapter((XoActivity)getActivity(), mGroup, mContactsToInvite, mContactsToKick);
            mAdapter.onCreate();
            mAdapter.onResume();
            mAdapter.setFilter(new ContactsAdapter.Filter() {
                @Override
                public boolean shouldShow(TalkClientContact contact) {
                    return mCurrentContactsInGroup.contains(contact) || (contact.isClient() && contact.isClientRelated());
                }
            });
        }
        mAdapter.requestReload();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_manage_group_title);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                LOG.debug("onClick(Ok)");
                updateMemberships();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                LOG.debug("onClick(Cancel)");
                dialog.dismiss();
            }
        });
        builder.setAdapter(mAdapter, null);

        final AlertDialog dialog = builder.create();
        dialog.getListView().setItemsCanFocus(false);
        dialog.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                LinearLayout contactView = (LinearLayout) view;
                CheckedTextView checkedTextView = (CheckedTextView)contactView.findViewById(R.id.contact_name_checked);
                checkedTextView.setChecked(!checkedTextView.isChecked());

                Object object = mAdapter.getItem(index);
                if (object != null && object instanceof TalkClientContact) {
                    TalkClientContact contact = (TalkClientContact)object;
                    if (checkedTextView.isChecked()) {

                        mContactsToInvite.add(contact);

                        if (mContactsToKick.contains(contact)) {
                            mContactsToKick.remove(contact);
                        }

                    } else {

                        mContactsToKick.add(contact);

                        if (mContactsToInvite.contains(contact)) {
                            mContactsToInvite.remove(contact);
                        }
                    }
                }
            }
        });

        return dialog;
    }

    private void updateMemberships() {
        for (TalkClientContact contact : mContactsToInvite) {
            ((XoActivity) getActivity()).getXoClient().inviteClientToGroup(mGroup.getGroupId(), contact.getClientId());
        }
        for (TalkClientContact contact : mContactsToKick) {
            ((XoActivity) getActivity()).getXoClient().kickClientFromGroup(mGroup.getGroupId(), contact.getClientId());
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if(mAdapter != null) {
            mAdapter.onPause();
            mAdapter.onDestroy();
        }
    }
}
