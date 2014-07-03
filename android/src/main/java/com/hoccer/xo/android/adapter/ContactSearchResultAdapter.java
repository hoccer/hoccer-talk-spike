package com.hoccer.xo.android.adapter;

import android.view.View;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by nico on 03/07/2014.
 */
public class ContactSearchResultAdapter extends ContactsAdapter {


    public ContactSearchResultAdapter(XoActivity activity) {
        super(activity);
        setShowTokens(false);
    }

    @Override
    protected int getClientLayout() {
        return R.layout.item_contact_search_result;
    }

    @Override
    protected int getGroupLayout() {
        return R.layout.item_contact_search_result;
    }

    @Override
    protected int getSeparatorLayout() {
        return 0;
    }

    @Override
    protected int getTokenLayout() {
        return 0;
    }

    @Override
    protected void updateContact(View view, TalkClientContact contact) {
        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        AvatarView avatarView = (AvatarView) view.findViewById(R.id.contact_icon);
        nameView.setText(contact.getName());
        TextView typeView = (TextView) view.findViewById(R.id.contact_type);

        avatarView.setContact(contact);
        if (contact.isGroup()) {
            if (contact.isGroupInvited()) {
                typeView.setText(R.string.common_group_invite);
            } else {
                typeView.setText(R.string.common_group);
            }
        }

    }

    @Override
    protected void updateToken(View view, TalkClientSmsToken token) {

    }
}
