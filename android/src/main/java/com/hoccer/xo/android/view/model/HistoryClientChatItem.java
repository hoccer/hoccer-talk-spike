package com.hoccer.xo.android.view.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.adapter.SearchAdapter;

public class HistoryClientChatItem extends ClientChatItem implements SearchAdapter.Searchable {

    public HistoryClientChatItem(TalkClientContact contact, Context context) {
        super(contact, context);
    }

    @Override
    public View getView(View view, ViewGroup parent) {
        if (view == null || view.getTag() == null || (Integer) view.getTag() != ChatItem.TYPE_CLIENT_HISTORY) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_chat_client, null);
            view.setTag(ChatItem.TYPE_CLIENT_HISTORY);
        }

        return super.updateView(view);
    }
}
