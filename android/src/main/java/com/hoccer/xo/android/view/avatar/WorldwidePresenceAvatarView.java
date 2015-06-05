package com.hoccer.xo.android.view.avatar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.talk.model.TalkPresence;

public class WorldwidePresenceAvatarView extends PresenceAvatarView {

    private final View mPresenceIndicatorOffline;

    public WorldwidePresenceAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs, R.layout.layout_avatar_worldwide_presence);
        mPresenceIndicatorOffline = this.findViewById(R.id.presence_indicator_view_offline);
    }

    protected void showPresenceIndicatorActive() {
        mPresenceIndicatorActive.setVisibility(View.VISIBLE);
        mPresenceIndicatorInactive.setVisibility(View.INVISIBLE);
        mPresenceIndicatorOffline.setVisibility(View.INVISIBLE);
    }

    protected void showPresenceIndicatorInactive() {
        mPresenceIndicatorActive.setVisibility(View.INVISIBLE);
        mPresenceIndicatorInactive.setVisibility(View.VISIBLE);
        mPresenceIndicatorOffline.setVisibility(View.INVISIBLE);
    }

    protected void showPresenceIndicatorOffline() {
        mPresenceIndicatorActive.setVisibility(View.INVISIBLE);
        mPresenceIndicatorInactive.setVisibility(View.VISIBLE);
        mPresenceIndicatorOffline.setVisibility(View.VISIBLE);
    }
}
