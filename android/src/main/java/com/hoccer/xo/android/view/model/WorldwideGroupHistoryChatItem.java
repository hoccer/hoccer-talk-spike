package com.hoccer.xo.android.view.model;

import android.view.View;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.SearchAdapter;
import com.hoccer.xo.android.view.avatar.AvatarView;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class WorldwideGroupHistoryChatItem extends ChatItem implements SearchAdapter.Searchable {

    private static final Logger LOG = Logger.getLogger(NearbyGroupHistoryChatItem.class);

    @Nullable
    private List<TalkClientMessage> mMessages;

    @Nullable
    private Date mLastMessageTimeStamp;
    private String mLastMessageText = "";

    public WorldwideGroupHistoryChatItem() {
        update();
    }

    @Override
    public void update() {
        try {
            mMessages = XoApplication.get().getXoClient().getDatabase().getAllWorldwideGroupMessages();
            mUnseenMessageCount = 0;
            for (TalkClientMessage worldwideMessage : mMessages) {
                if (worldwideMessage.isIncoming() && !worldwideMessage.isSeen()) {
                    mUnseenMessageCount++;
                }
            }
        } catch (SQLException e) {
            LOG.error("Error while retrieving all worldwide group messages: ", e);
        }
        if (mMessages != null && !mMessages.isEmpty()) {
            TalkClientMessage message = mMessages.get(mMessages.size() - 1);
            if (message != null) {
                mLastMessageTimeStamp = message.getTimestamp();
                mLastMessageText = message.getText();
            }
        }
    }

    @Override
    protected View updateView(View view) {
        AvatarView simpleAvatarView = (AvatarView) view.findViewById(R.id.contact_icon);
        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        TextView lastMessageTextView = (TextView) view.findViewById(R.id.contact_last_message);
        TextView lastMessageTimeView = (TextView) view.findViewById(R.id.contact_time);
        TextView unseenView = (TextView) view.findViewById(R.id.contact_unseen_messages);

        nameView.setText(R.string.worldwide_saved);
        setLastMessageTime(lastMessageTimeView);
        lastMessageTextView.setText(mLastMessageText);
        setUnseenMessages(unseenView);

        simpleAvatarView.setAvatarImage(R.drawable.avatar_world);
        simpleAvatarView.setClickable(false);

        return view;
    }

    @Override
    public Object getContent() {
        return "worldwideArchived";
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
