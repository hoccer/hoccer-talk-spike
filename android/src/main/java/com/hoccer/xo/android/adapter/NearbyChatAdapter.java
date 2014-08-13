package com.hoccer.xo.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NearbyChatAdapter extends ChatAdapter {

    private XoActivity mXoActivity;

    public NearbyChatAdapter(ListView listView, XoActivity activity) {
        super(listView, activity, null);
        mXoActivity = activity;
    }

    @Override
    protected void initialize() {
        int totalMessageCount = 0;
        try {
            totalMessageCount = (int) mDatabase.getNearbyMessageCount();
        } catch (SQLException e) {
            LOG.error("SQLException while loading message count in nearby ");
        }
        mChatMessageItems = new ArrayList<ChatMessageItem>(totalMessageCount);
        for (int i = 0; i < totalMessageCount; i++) {
            mChatMessageItems.add(null);
        }
        loadNextMessages(mChatMessageItems.size() - (int) BATCH_SIZE);
    }

    @Override
    public synchronized void loadNextMessages(int offset) {
        try {
            if (offset < 0) {
                offset = 0;
            }
            final List<TalkClientMessage> messagesBatch = mDatabase.findNearbyMessages(BATCH_SIZE, offset);
            for (int i = 0; i < messagesBatch.size(); i++) {
                ChatMessageItem messageItem = getItemForMessage(messagesBatch.get(i));
                mChatMessageItems.set(offset + i, messageItem);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        } catch (SQLException e) {
            LOG.error("SQLException while batch retrieving messages for nearby", e);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ChatMessageItem chatItem = getItem(position);
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
        if (convertView == null || (convertView.getTag()!= null && convertView.getTag().equals(TalkClientMessage.TYPE_SEPARATOR))) {
            convertView = chatItem.createViewForMessage();
        } else {
            convertView = chatItem.recycleViewForMessage(convertView);
        }
        return convertView;
    }

    @Override
    public void onReloadRequest() {
        super.onReloadRequest();
    }
}
