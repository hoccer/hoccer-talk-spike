package com.hoccer.xo.android.view;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.artcom.hoccer.R;

public class Placeholder {

    private final int mImageId;
    private final int mTextId;

    public Placeholder(int imageId, int textId) {
        mImageId = imageId;
        mTextId = textId;
    }

    public void applyToView(View view) {
        applyToView(view, null);
    }

    public void applyToView(View view, View.OnClickListener onClickListener) {
        ImageView placeholderImage = (ImageView) view.findViewById(R.id.iv_contacts_placeholder);
        TextView placeholderText = (TextView) view.findViewById(R.id.tv_contacts_placeholder);

        setupPlaceholderImage(onClickListener, placeholderImage);
        setupPlaceholderText(view, placeholderText);
    }

    private void setupPlaceholderText(View view, TextView placeholderText) {
        String text = view.getResources().getString(mTextId);
        placeholderText.setMovementMethod(LinkMovementMethod.getInstance());
        placeholderText.setText(Html.fromHtml(text));
    }

    private void setupPlaceholderImage(View.OnClickListener onClickListener, ImageView placeholderImage) {
        placeholderImage.setImageResource(mImageId);
        placeholderImage.setOnClickListener(onClickListener);
    }
}
