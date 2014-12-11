package com.hoccer.xo.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.xo.android.XoApplication;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

/**
 * A view holding an AspectImageView and a presence indicator.
 */
public class AvatarView extends LinearLayout implements IXoContactListener {

    private String mDefaultAvatarImageUrl;
    private DisplayImageOptions mDefaultOptions;
    private float mCornerRadius = 0.0f;
    private AspectImageView mAvatarImage;
    private View mPresenceIndicatorActive;
    private View mPresenceIndicatorInactive;

    private TalkClientContact mContact;

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        applyAttributes(attrs);
        initializeView();
    }

    private void initializeView() {
        View layout = LayoutInflater.from(getContext()).inflate(R.layout.view_avatar, null);
        addView(layout);

        mAvatarImage = (AspectImageView) this.findViewById(R.id.avatar_image);
        mPresenceIndicatorActive = this.findViewById(R.id.presence_indicator_view_active);
        mPresenceIndicatorInactive = this.findViewById(R.id.presence_indicator_view_inactive);

        float scale = getResources().getDisplayMetrics().density;
        int pixel = (int) (mCornerRadius * scale + 0.5f);
        if (isInEditMode()) {
            mDefaultOptions = new DisplayImageOptions.Builder()
                    .displayer(new RoundedBitmapDisplayer(pixel)).build();
        } else {
            mDefaultOptions = new DisplayImageOptions.Builder()
                    .cloneFrom(XoApplication.getContentImageOptions())
                    .displayer(new RoundedBitmapDisplayer(pixel)).build();
        }
        setAvatarImage(mDefaultAvatarImageUrl);
    }

    private void applyAttributes(AttributeSet attributes) {
        TypedArray a = getContext().getTheme()
                .obtainStyledAttributes(attributes, R.styleable.AvatarView, 0, 0);
        try {
            mDefaultAvatarImageUrl = "drawable://" + a.getResourceId(R.styleable.AvatarView_defaultAvatarImageUrl, R.drawable.avatar_default_contact);
            mCornerRadius = a.getFloat(R.styleable.AvatarView_cornerRadius, 0.0f);
        } finally {
            a.recycle();
        }
    }


    public void setContact(TalkClientContact contact) {
        mContact = contact;
        updateAvatar();
        updatePresence();
        XoApplication.getXoClient().registerContactListener(this);
    }

    private void updateAvatar() {
        if (mContact == null) {
            resetAvatar();
            return;
        }
        IContentObject avatar = mContact.getAvatar();
        String avatarUri = avatar == null ? null : avatar.getContentDataUrl();

        if (avatarUri == null) {
            if (mContact.isGroup()) {
                if (mContact.getGroupPresence() != null && mContact.getGroupPresence().isTypeNearby()) {
                    setAvatarImage(R.drawable.avatar_default_location);
                } else {
                    setAvatarImage(R.drawable.avatar_default_group);
                }
            } else {
                setAvatarImage(R.drawable.avatar_default_contact);
            }
        } else {
            setAvatarImage(avatarUri);
        }
    }

    private void resetAvatar() {
        setAvatarImage(null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAvatar();
        updatePresence();
    }

    /**
     * Sets the avatar image. Value can be null. Uses default avatar image url instead (if
     * specified).
     *
     * @param avatarImageUrl Url of the given image resource  to load.
     */
    public void setAvatarImage(final String avatarImageUrl) {
        post(new Runnable() {
            @Override
            public void run() {
                if (isInEditMode()) {
                    ImageView avatar = (ImageView) findViewById(R.id.avatar_image);
                    avatar.setImageResource(R.drawable.avatar_default_contact);
                } else {
                    mAvatarImage.setVisibility(View.VISIBLE);
                    if (avatarImageUrl != null) {
                        ImageLoader.getInstance()
                                .displayImage(avatarImageUrl, mAvatarImage, mDefaultOptions, null);
                    } else if (mDefaultAvatarImageUrl != null) {
                        ImageLoader.getInstance()
                                .displayImage(mDefaultAvatarImageUrl, mAvatarImage, mDefaultOptions,
                                        null);
                    } else {
                        mAvatarImage.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    public void setAvatarImage(int resourceId) {
        setAvatarImage("drawable://" + resourceId);
    }

    private void updatePresence() {
        post(new Runnable() {
            @Override
            public void run() {

                if (mContact != null && mContact.isClient()) {
                    TalkPresence presence = mContact.getClientPresence();
                    if (presence != null) {
                        if (presence.isConnected()) {
                            if (presence.isPresent()) {
                                showPresenceIndicatorActive();
                            } else {
                                showPresenceIndicatorInactive();
                            }
                            return;
                        }
                    }
                }
                hidePresenceIndicator();
            }
        });
    }

    private void showPresenceIndicatorActive() {
        mPresenceIndicatorActive.setVisibility(View.VISIBLE);
        mPresenceIndicatorInactive.setVisibility(View.INVISIBLE);
    }

    private void showPresenceIndicatorInactive() {
        mPresenceIndicatorActive.setVisibility(View.INVISIBLE);
        mPresenceIndicatorInactive.setVisibility(View.VISIBLE);
    }

    private void hidePresenceIndicator() {
        mPresenceIndicatorActive.setVisibility(View.INVISIBLE);
        mPresenceIndicatorInactive.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        updatePresence();
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
    }
}
