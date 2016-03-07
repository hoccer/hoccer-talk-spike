package com.hoccer.xo.android.view.avatar;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.profile.client.ClientProfileActivity;
import com.hoccer.xo.android.profile.group.GroupProfileActivity;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.AspectImageView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import org.apache.log4j.Logger;

public abstract class AvatarView extends LinearLayout {

    private static final Logger LOG = Logger.getLogger(XoClient.class);

    private Uri mDefaultAvatarImageUri;
    private DisplayImageOptions mDefaultOptions;
    private float mCornerRadius;
    private AspectImageView mAvatarImage;

    protected TalkClientContact mContact;

    public AvatarView(Context context, AttributeSet attrs, int layoutId) {
        super(context, attrs);
        setAttributes(attrs);
        initializeView(layoutId);
    }

    private void setAttributes(AttributeSet attributes) {
        TypedArray a = getContext().getTheme()
                .obtainStyledAttributes(attributes, R.styleable.SimpleAvatarView, 0, 0);
        try {
            mDefaultAvatarImageUri = Uri.parse("drawable://" + a.getResourceId(R.styleable.SimpleAvatarView_defaultAvatarImageUrl, R.drawable.avatar_contact));
            mCornerRadius = a.getFloat(R.styleable.SimpleAvatarView_cornerRadius, 0.0f);
        } finally {
            a.recycle();
        }
    }

    private void initializeView(int layoutId) {
        View layout = LayoutInflater.from(getContext()).inflate(layoutId, null);
        addView(layout);

        mAvatarImage = (AspectImageView) this.findViewById(R.id.avatar_image);

        float scale = getResources().getDisplayMetrics().density;
        int pixel = (int) (mCornerRadius * scale + 0.5f);
        if (isInEditMode()) {
            mDefaultOptions = new DisplayImageOptions.Builder()
                    .displayer(new RoundedBitmapDisplayer(pixel)).build();
        } else {
            mDefaultOptions = new DisplayImageOptions.Builder()
                    .cloneFrom(XoApplication.getImageOptions())
                    .displayer(new RoundedBitmapDisplayer(pixel)).build();
        }
        setAvatarImage(mDefaultAvatarImageUri);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if (mContact.isGroup()) {
                    intent = new Intent(getContext(), GroupProfileActivity.class)
                            .setAction(GroupProfileActivity.ACTION_SHOW)
                            .putExtra(GroupProfileActivity.EXTRA_CLIENT_CONTACT_ID, mContact.getClientContactId());
                } else {
                    intent = new Intent(getContext(), ClientProfileActivity.class)
                            .setAction(ClientProfileActivity.ACTION_SHOW)
                            .putExtra(ClientProfileActivity.EXTRA_CLIENT_CONTACT_ID, mContact.getClientContactId());
                }
                getContext().startActivity(intent);
            }
        });
    }

    public void setContact(TalkClientContact contact) {
        mContact = contact;
        try {
            updateAvatar();
        } catch (Exception e){
            LOG.error("Error updating avatar", e);
        }
    }

    private void updateAvatar() {
        if (mContact == null) {
            resetAvatar();
            return;
        }
        XoTransfer avatar = mContact.getAvatar();

        if (avatar == null || avatar.getFilePath() == null) {
            if (mContact.isGroup()) {
                if (mContact.getGroupPresence() != null && mContact.getGroupPresence().isTypeNearby()) {
                    setAvatarImage(R.drawable.avatar_location);
                } else if (mContact.getGroupPresence() != null && mContact.getGroupPresence().isTypeWorldwide()) {
                    setAvatarImage(R.drawable.avatar_world);
                } else {
                    setAvatarImage(R.drawable.avatar_group);
                }
            } else {
                setAvatarImage(R.drawable.avatar_contact);
            }
        } else {
            Uri avatarUri = UriUtils.getAbsoluteFileUri(avatar.getFilePath());
            setAvatarImage(avatarUri);
        }

        updateAvatarBlocked();
    }

    private void updateAvatarBlocked() {
        if (mContact.isClient() && mContact.getClientRelationship() != null && mContact.getClientRelationship().isBlocked()) {
            if (findViewWithTag("blockedLayout") != null) {
                return;
            }

            View backgroundLayerView = new View(getContext());
            backgroundLayerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            backgroundLayerView.setBackgroundResource(R.drawable.shape_round_black_transparency);

            ImageView imageView = new ImageView(getContext());
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            imageView.setLayoutParams(params);
            imageView.setImageResource(R.drawable.ic_action_block);

            RelativeLayout blockedLayout = new RelativeLayout(getContext());
            blockedLayout.setTag("blockedLayout");
            blockedLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            blockedLayout.addView(backgroundLayerView);
            blockedLayout.addView(imageView);

            ViewGroup parent = (ViewGroup) mAvatarImage.getParent();
            int avatarImageIndex = parent.indexOfChild(mAvatarImage);
            parent.addView(blockedLayout, avatarImageIndex + 1);

        } else if (mContact.isClient() && mContact.getClientRelationship() != null) {
            View blockedLayout = findViewWithTag("blockedLayout");
            ((ViewGroup) mAvatarImage.getParent()).removeView(blockedLayout);
        }
    }

    private void resetAvatar() {
        setAvatarImage(null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            updateAvatar();
        } catch (Exception e){
            LOG.error("Error updating avatar", e);
        }
    }

    /**
     * Sets the avatar image. Value can be null. Uses default avatar image url instead (if
     * specified).
     *
     * @param avatarImageUri Url of the given image resource  to load.
     */
    public void setAvatarImage(final Uri avatarImageUri) {
        if (isInEditMode()) {
            ImageView avatar = (ImageView) findViewById(R.id.avatar_image);
            avatar.setImageResource(R.drawable.avatar_contact);
        } else {
            mAvatarImage.setVisibility(View.VISIBLE);
            if (avatarImageUri != null) {
                ImageLoader.getInstance()
                        .displayImage(avatarImageUri.toString(), mAvatarImage, mDefaultOptions, null);
            } else if (mDefaultAvatarImageUri != null) {
                ImageLoader.getInstance()
                        .displayImage(mDefaultAvatarImageUri.toString(), mAvatarImage, mDefaultOptions,
                                null);
            } else {
                mAvatarImage.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void setAvatarImage(int resourceId) {
        setAvatarImage(Uri.parse("drawable://" + resourceId));
    }

    public static AvatarView inflate(TalkClientContact contact, Context context) {
        AvatarView avatarView = (AvatarView) LayoutInflater.from(context).inflate(getLayoutResource(contact), null);
        avatarView.setContact(contact);
        return avatarView;
    }

    public static int getLayoutResource(TalkClientContact contact) {
        int layoutId;

        if (contact.isWorldwide() && !contact.isClientFriend()) {
            layoutId = R.layout.view_avatar_client_presence_worldwide;
        } else if (contact.isNearby() && !contact.isClientFriend()) {
            layoutId = R.layout.view_avatar_client_presence_nearby;
        } else if (contact.isKept() && contact.isNearbyAcquaintance()) {
            layoutId = R.layout.view_avatar_client_acquaintance_nearby;
        } else if (contact.isKept() && contact.isWorldwideAcquaintance()) {
            layoutId = R.layout.view_avatar_client_acquaintance_worldwide;
        } else if (contact.isKept() || contact.isKeptGroup()) {
            layoutId = R.layout.view_avatar_client_kept;
        } else if (contact.isWorldwideGroup()) {
            layoutId = R.layout.view_avatar_group_worldwide;
        } else {
            layoutId = R.layout.view_avatar_presence;
        }

        return layoutId;
    }
}
