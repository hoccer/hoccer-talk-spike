package com.hoccer.xo.android.view.model;

import android.view.View;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class NearbyGroupContactItem extends BaseContactItem {

    private static final Logger LOG = Logger.getLogger(TalkClientContactItem.class);

    @Nullable
    private List<TalkClientMessage> mNearbyMessages;

    @Nullable
    private Date mLastMessageTimeStamp = null;
    private String mLastMessageText = "";

    public NearbyGroupContactItem(XoActivity activity) {
        super(activity);
        update();
    }

    @Override
    public void update() {
        try {
            mNearbyMessages = mXoActivity.getXoDatabase().getAllNearbyGroupMessages();
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
    protected View configure(View view) {
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
    public long getTimeStamp() {
        return -1000;
    }

    private void setLastMessageTime(TextView lastMessageTime) {
        if (mLastMessageTimeStamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE HH:mm");
            lastMessageTime.setText(sdf.format(mLastMessageTimeStamp));
        }
    }
}