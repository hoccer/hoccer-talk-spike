package com.hoccer.xo.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.talk.model.TalkGroupPresence;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.chat.MessageItem;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends ChatAdapter {

    private static final Logger LOG = Logger.getLogger(HistoryAdapter.class);
    private String mEnvironmentType;

    public HistoryAdapter(ListView listView, XoActivity activity, TalkClientContact contact) {
        super(listView, activity, contact);
    }

    public HistoryAdapter(ListView listView, XoActivity activity, String environmentType) {
        super(listView, activity);
        mEnvironmentType = environmentType;
        initializeEnvironmentGroupHistory();
    }

    private void initializeEnvironmentGroupHistory() {
        List<TalkClientMessage> messages = new ArrayList<TalkClientMessage>();
        try {
            if (TalkEnvironment.TYPE_NEARBY.equals(mEnvironmentType)) {
                messages = mDatabase.getAllNearbyGroupMessages();
            } else if (TalkEnvironment.TYPE_WORLDWIDE.equals(mEnvironmentType)) {
                messages = mDatabase.getAllWorldwideGroupMessages();
            }
        } catch (SQLException e) {
            LOG.error("SQLException while batch retrieving messages for environment", e);
        }

        mMessageItems = new ArrayList<MessageItem>(messages.size());
        for (TalkClientMessage message : messages) {
            MessageItem messageItem = getItemForMessage(message);
            mMessageItems.add(messageItem);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final MessageItem chatItem = getItem(position);
        if (chatItem.isSeparator()) {
            convertView = ((LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.item_chat_separator, null);
            TextView tv = (TextView) convertView.findViewById(R.id.tv_header);
            tv.setText(chatItem.getText());
            convertView.setTag(TalkClientMessage.TYPE_SEPARATOR);
            return convertView;
        }
        if (!chatItem.getMessage().isSeen()) {
            markMessageAsSeen(chatItem.getMessage());
        }
        if (convertView == null || (convertView.getTag() != null && convertView.getTag().equals(TalkClientMessage.TYPE_SEPARATOR))) {
            convertView = chatItem.createViewForMessage();
        } else {
            convertView = chatItem.recycleViewForMessage(convertView);
        }
        return convertView;
    }

    @Override
    protected boolean isMessageRelevant(TalkClientMessage message) {
        TalkClientContact conversationContact = message.getConversationContact();
        if (conversationContact != null && conversationContact.getContactType() != null) {
            if (conversationContact.isGroup()) {
                TalkGroupPresence groupPresence = conversationContact.getGroupPresence();
                if (groupPresence != null && groupPresence.isTypeNearby()) {
                    return true;
                }
            }
        }

        return false;
    }
}
