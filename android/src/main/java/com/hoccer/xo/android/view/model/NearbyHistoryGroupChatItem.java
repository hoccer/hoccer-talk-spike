package com.hoccer.xo.android.view.model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.SearchAdapter;
import com.hoccer.xo.android.view.AvatarView;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NearbyHistoryGroupChatItem extends ChatItem implements SearchAdapter.Searchable {

    private static final Logger LOG = Logger.getLogger(NearbyHistoryGroupChatItem.class);

    @Nullable
    private List<TalkClientMessage> mNearbyMessages;

    @Nullable
    private Date mLastMessageTimeStamp;
    private String mLastMessageText = "";

    public NearbyHistoryGroupChatItem() {
        update();
    }

    @Override
    public void update() {
        try {
            mNearbyMessages = XoApplication.get().getXoClient().getDatabase().getAllNearbyGroupMessages();
            mUnseenMessageCount = 0;
            for (TalkClientMessage nearbyMessage : mNearbyMessages) {
                if (nearbyMessage.isIncoming() && !nearbyMessage.isSeen()) {
                    mUnseenMessageCount++;
                }
            }
        } catch (SQLException e) {
            LOG.error("Error while retrieving all nearby group messages: ", e);
        }
        if (mNearbyMessages != null && !mNearbyMessages.isEmpty()) {
            TalkClientMessage message = mNearbyMessages.get(mNearbyMessages.size() - 1);
            if (message != null) {
                mLastMessageTimeStamp = message.getTimestamp();
                mLastMessageText = message.getText();
            }
        }
    }

    @Override
    public View getView(View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client_chat_client, null);
        }

        AvatarView simpleAvatarView = (AvatarView) view.findViewById(R.id.contact_icon);
        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        TextView lastMessageTextView = (TextView) view.findViewById(R.id.contact_last_message);
        TextView lastMessageTimeView = (TextView) view.findViewById(R.id.contact_time);
        TextView unseenView = (TextView) view.findViewById(R.id.contact_unseen_messages);

        nameView.setText(R.string.nearby_saved);
        setLastMessageTime(lastMessageTimeView);
        lastMessageTextView.setText(mLastMessageText);
        setUnseenMessages(unseenView);

        simpleAvatarView.setAvatarImage(R.drawable.avatar_default_location);
        simpleAvatarView.setClickable(false);

        return view;
    }

    @Override
    public Object getContent() {
        return "nearbyArchived";
    }

    @Override
    public long getMessageTimeStamp() {
        return -1000;
    }

    @Override
    public long getContactCreationTimeStamp() {
        return 0;
    }

    private void setLastMessageTime(TextView lastMessageTime) {
        if (mLastMessageTimeStamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE HH:mm");
            lastMessageTime.setText(sdf.format(mLastMessageTimeStamp));
        }
    }

    @Override
    public boolean matches(String query) {
        // should not be searched for. Interface implemented for compatibility reasons
        return false;
    }
}
