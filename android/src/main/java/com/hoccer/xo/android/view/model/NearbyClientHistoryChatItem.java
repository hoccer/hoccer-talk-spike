package com.hoccer.xo.android.view.model;


import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.adapter.SearchAdapter;
import com.hoccer.xo.android.view.AvatarView;
import org.jetbrains.annotations.Nullable;

public class NearbyClientHistoryChatItem extends BaseChatItem implements SearchAdapter.Searchable{

    @Nullable
    private String mLastMessageText = "";

    @Override
    public void update() {
    }

    @Override
    protected View configure(Context context, View view) {
        AvatarView avatarView = (AvatarView) view.findViewById(R.id.contact_icon);
        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        TextView lastMessageTextView = (TextView) view.findViewById(R.id.contact_last_message);
        TextView unseenView = (TextView) view.findViewById(R.id.contact_unseen_messages);

        nameView.setText(R.string.nearby_saved);
        lastMessageTextView.setText(mLastMessageText);
        setUnseenMessages(unseenView);

        avatarView.setAvatarImage(R.drawable.avatar_default_location);
        avatarView.setClickable(false);

        return view;
    }
    @Override
    public Object getContent() {
        return null;
    }

    @Override
    public long getMessageTimeStamp() {
        return 0;
    }

    @Override
    public long getContactCreationTimeStamp() {
        return 0;
    }

    @Override
    public boolean matches(String query) {
        return false;
    }
}
