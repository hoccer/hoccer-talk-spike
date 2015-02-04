package com.hoccer.xo.android.content;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.activity.MultiImagePickerActivity;
import com.hoccer.xo.android.content.selector.IContentCreator;
import com.hoccer.xo.android.content.selector.ImageSelector;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class MultiImageSelector extends ImageSelector {

    private static final Logger LOG = Logger.getLogger(MultiImageSelector.class);

    public MultiImageSelector(Context context) {
        super(context);
        setName(context.getResources().getString(R.string.content_multi_images));
        setIcon(ColoredDrawable.getFromCache(R.drawable.ic_attachment_select_image, R.color.primary));
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

    public ArrayList<SelectedContent> createObjectsFromSelectionResult(Context context, Intent intent) {
        ArrayList<SelectedContent> result = new ArrayList<SelectedContent>();
        if (!isValidIntent(context, intent)) {
            return result;
        }

        String[] uris = intent.getStringArrayExtra(MultiImagePickerActivity.EXTRA_IMAGES);
        for (String uri : uris) {
            IContentCreator creator = findContentCreator(Uri.parse(uri));
            if (creator == null) {
                LOG.warn("No IContentCreator found for url '" + uri + "'");
                return result;
            }

            Intent dataIntent = new Intent();
            dataIntent.setDataAndType(Uri.parse(uri), "image/*");
            SelectedContent selectedImage = creator.apply(context, dataIntent);
            if (selectedImage != null) {
                result.add(selectedImage);
            }
        }

        return result;
    }
}
