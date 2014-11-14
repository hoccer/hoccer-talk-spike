package com.hoccer.xo.android.view.model;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.SearchAdapter;
import com.hoccer.xo.android.view.AvatarView;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class NearbyGroupChatItem extends BaseChatItem implements SearchAdapter.Searchable{

    private static final Logger LOG = Logger.getLogger(NearbyGroupChatItem.class);

    @Nullable
    private List<TalkClientMessage> mNearbyMessages;

    @Nullable
    private Date mLastMessageTimeStamp = null;
    private String mLastMessageText = "";

    public NearbyGroupChatItem() {
        update();
    }

    @Override
    public void update() {
        try {
            mNearbyMessages = XoApplication.getXoClient().getDatabase().getAllNearbyGroupMessages();
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
    protected View configure(Context context, View view) {
        AvatarView avatarView = (AvatarView) view.findViewById(R.id.contact_icon);
        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        TextView typeView = (TextView) view.findViewById(R.id.contact_type);
        TextView lastMessageTextView = (TextView) view.findViewById(R.id.contact_last_message);
        TextView lastMessageTimeView = (TextView) view.findViewById(R.id.contact_time);

        nameView.setText(R.string.nearby_saved);
        typeView.setText(R.string.common_group);
        setLastMessageTime(lastMessageTimeView);
        lastMessageTextView.setText(mLastMessageText);

        avatarView.setAvatarImage(R.drawable.avatar_default_location);
        avatarView.setClickable(false);

        return view;
    }

    @Override
    public Object getContent() {
        return new String("nearbyArchived");
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
