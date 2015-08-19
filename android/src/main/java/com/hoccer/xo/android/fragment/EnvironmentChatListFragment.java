package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.activity.ChatActivity;
import com.hoccer.xo.android.adapter.EnvironmentChatListAdapter;
import com.hoccer.xo.android.base.PagerListFragment;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.Placeholder;

public abstract class EnvironmentChatListFragment extends PagerListFragment {

    protected Placeholder mPlaceholder;
    protected EnvironmentChatListAdapter mListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        mPlaceholder.applyToView(view);
        return view;
    }
    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        super.onListItemClick(listView, v, position, id);
        Object item = listView.getItemAtPosition(position);
        if (item instanceof TalkClientContact) {
            TalkClientContact contact = (TalkClientContact) item;
            showContactConversation(contact);
        }
    }

    public void showContactConversation(TalkClientContact contact) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, contact.getClientContactId());
        startActivity(intent);
    }
}
