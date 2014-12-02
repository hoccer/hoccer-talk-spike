package com.hoccer.xo.android.content;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.activity.MultiImagePickerActivity;
import com.hoccer.xo.android.content.contentselectors.IContentCreator;
import com.hoccer.xo.android.content.contentselectors.ImageSelector;
import com.hoccer.xo.android.util.ColorSchemeManager;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class MultiImageSelector extends ImageSelector {

    private static final Logger LOG = Logger.getLogger(MultiImageSelector.class);
    private final String mName;
    private final Drawable mIcon;

    public MultiImageSelector(Context context) {
        super(context);
        mName = context.getResources().getString(R.string.content_multi_images);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context.getResources(), R.drawable.ic_attachment_select_image, true);
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public Drawable getContentIcon() {
        return mIcon;
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        String[] arrayExtra = intent.getStringArrayExtra(MultiImagePickerActivity.EXTRA_IMAGES);
        return arrayExtra != null && arrayExtra.length > 0;
    }

    @Override
    public Intent createSelectionIntent(Context context) {
        return new Intent(context, MultiImagePickerActivity.class);
    }

    public ArrayList<IContentObject> createObjectsFromSelectionResult(Context context, Intent intent) {
        ArrayList<IContentObject> result = new ArrayList<IContentObject>();
        if (!isValidIntent(context, intent)) {
            return result;
        }

        String[] uris = intent.getStringArrayExtra(MultiImagePickerActivity.EXTRA_IMAGES);
        for (String uri : uris) {
            IContentCreator creator = findContentObjectCreator(Uri.parse(uri));
            if (creator == null) {
                LOG.warn("No IContentCreator found for url '" + uri + "'");
                return result;
            }

            Intent dataIntent = new Intent();
            dataIntent.setDataAndType(Uri.parse(uri), "image/*");
            SelectedContent selectedContent = creator.apply(context, dataIntent);
            if (selectedContent != null) {
                result.add(selectedContent);
            }
        }

        return result;
    }
}
