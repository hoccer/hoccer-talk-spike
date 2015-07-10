package com.hoccer.xo.android.adapter;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.avatar.PresenceAvatarView;

import java.util.ArrayList;
import java.util.List;


public class ContactSearchResultAdapter extends ContactsAdapter {

    private final List<TalkClientContact> mFoundContacts = new ArrayList<TalkClientContact>();
    private String mLastQuery = "";

    public ContactSearchResultAdapter(XoActivity activity) {
        super(activity);
    }

    public void searchForContactsByName(String query) {
        mFoundContacts.clear();
        mLastQuery = query.toLowerCase();

        if (!query.isEmpty()) {
            for (TalkClientContact contact : mContacts) {
                // ignore self
                if (contact.isSelf()) {
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
    protected int getSeparatorLayout() {
        return 0;
    }

    @Override
    protected void updateContact(View view, TalkClientContact contact) {
        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        nameView.setText(getHighlightedSearchResult(contact.getName()));
    }

    private Spannable getHighlightedSearchResult(String text) {
        Spannable result = new SpannableString(text);
        result.setSpan(new ForegroundColorSpan(Color.BLACK), 0, mLastQuery.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }
}
