package com.hoccer.xo.android.view;

import android.content.res.Resources;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.artcom.hoccer.R;

public class Placeholder {

    private final int mImageId;
    private final int mHeadImageId;
    private final int mTextId;

    public Placeholder(int imageId, int headImageId, int textId) {
        mImageId = imageId;
        mHeadImageId = headImageId;
        mTextId = textId;
    }

    public void applyToView(View view) {
        ImageView placeholderImageFrame = (ImageView) view.findViewById(R.id.iv_contacts_placeholder_frame);
        ImageView placeholderImage = (ImageView) view.findViewById(R.id.iv_contacts_placeholder);
        TextView placeholderText = (TextView) view.findViewById(R.id.tv_contacts_placeholder);
        Resources resources = view.getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            placeholderImageFrame.setBackground(resources.getDrawable(mImageId));
            placeholderImage.setBackground(ColorSchemeManager.getInkedDrawable(mHeadImageId, R.color.primary));
        } else {
            placeholderImageFrame.setBackgroundDrawable(resources.getDrawable(mImageId));
            placeholderImage.setBackgroundDrawable(ColorSchemeManager.getInkedDrawable(mHeadImageId, R.color.primary));
        }

        String text = resources.getString(mTextId);
        placeholderText.setMovementMethod(LinkMovementMethod.getInstance());
        placeholderText.setText(Html.fromHtml(text));
    }
}
