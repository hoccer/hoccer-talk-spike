package com.hoccer.xo.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.util.UriUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

/**
 * A view holding an AspectImageView and a presence indicator.
 */
public class AvatarView extends LinearLayout {

    private Uri mDefaultAvatarImageUri;
    private DisplayImageOptions mDefaultOptions;
    private float mCornerRadius;
    private AspectImageView mAvatarImage;

    protected TalkClientContact mContact;

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setAttributes(attrs);
        initializeView();
    }

    private void setAttributes(AttributeSet attributes) {
        TypedArray a = getContext().getTheme()
                .obtainStyledAttributes(attributes, R.styleable.AvatarView, 0, 0);
        try {
            mDefaultAvatarImageUri = Uri.parse("drawable://" + a.getResourceId(R.styleable.AvatarView_defaultAvatarImageUrl, R.drawable.avatar_default_contact));
            mCornerRadius = a.getFloat(R.styleable.AvatarView_cornerRadius, 0.0f);
        } finally {
            a.recycle();
        }
    }

    private void initializeView() {
        View layout = LayoutInflater.from(getContext()).inflate(R.layout.view_avatar, null);
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
    }

    public void setContact(TalkClientContact contact) {
        mContact = contact;
        updateAvatar();
    }

    private void updateAvatar() {
        if (mContact == null) {
            resetAvatar();
            return;
        }
        XoTransfer avatar = mContact.getAvatar();
        Uri avatarUri = avatar == null ? null : UriUtils.getAbsoluteFileUri(avatar.getFilePath());

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
    }

    /**
     * Sets the avatar image. Value can be null. Uses default avatar image url instead (if
     * specified).
     *
     * @param avatarImageUri Url of the given image resource  to load.
     */
    public void setAvatarImage(final Uri avatarImageUri) {
        post(new Runnable() {
            @Override
            public void run() {
                if (isInEditMode()) {
                    ImageView avatar = (ImageView) findViewById(R.id.avatar_image);
                    avatar.setImageResource(R.drawable.avatar_default_contact);
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
        });
    }

    public void setAvatarImage(int resourceId) {
        setAvatarImage(Uri.parse("drawable://" + resourceId));
    }
}
