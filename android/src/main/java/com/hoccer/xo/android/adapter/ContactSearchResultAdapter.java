package com.hoccer.xo.android.adapter;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

import java.util.ArrayList;
import java.util.List;


public class ContactSearchResultAdapter extends ContactsAdapter {

    private List<TalkClientContact> mFoundContacts = new ArrayList<TalkClientContact>();
    private String mLastQuery = "";

    public ContactSearchResultAdapter(XoActivity activity) {
        super(activity);
        setShowTokens(false);
    }

    public void searchForContactsByName(String query) {
        mFoundContacts.clear();
        mLastQuery = query.toLowerCase();

        if (query.length() > 0) {
            for (TalkClientContact contact : mClientContacts) {
                // ignore self
                if(contact.isSelf()) {
                    continue;
                }

                // ignore nearby groups
                if (contact.isGroup() && contact.getGroupPresence() != null && contact.getGroupPresence().isTypeNearby()) {
                    continue;
                }

                String name = contact.getName().toLowerCase();
                if (name.startsWith(mLastQuery)) {
                    mFoundContacts.add(contact);
                }
            }
        } else {
            mFoundContacts.clear();
        }

    }

    @Override
    public Object getItem(int position) {
        return mFoundContacts.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_CLIENT;
    }

    @Override
    public int getCount() {
        return mFoundContacts.size();
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
    protected int getNearbyHistoryLayout() {
        return 0;
    }

    @Override
    protected void updateNearbyHistoryLayout(View v) {

    }

    @Override
    protected void updateContact(View view, TalkClientContact contact) {
        AvatarView avatarView = (AvatarView) view.findViewById(R.id.contact_icon);

        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        nameView.setText(getHighlightedSearchResult(contact.getName()));
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

    private Spannable getHighlightedSearchResult(String text) {
        Spannable result = new SpannableString(text);
        result.setSpan(new ForegroundColorSpan(Color.BLACK), 0, mLastQuery.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }
}
