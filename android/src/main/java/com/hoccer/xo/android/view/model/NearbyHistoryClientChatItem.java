package com.hoccer.xo.android.view.model;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.activity.GroupProfileActivity;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.SearchAdapter;

public class NearbyHistoryClientChatItem extends ClientChatItem implements SearchAdapter.Searchable {

    public NearbyHistoryClientChatItem(TalkClientContact contact, Context context) {
        super(contact, context);
    }

    @Override
    public View getView(View view, ViewGroup parent) {
        if(view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nearby_history_client_chat_client, null);
        }
        return super.getView(view, parent);
    }
}
