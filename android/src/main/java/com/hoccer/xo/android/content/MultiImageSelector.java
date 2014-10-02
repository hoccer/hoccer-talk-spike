package com.hoccer.xo.android.content;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.activity.MultiImagePickerActivity;
import com.hoccer.xo.android.content.contentselectors.IContentCreator;
import com.hoccer.xo.android.content.contentselectors.IContentSelector;
import com.hoccer.xo.android.content.contentselectors.ImageSelector;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MultiImageSelector extends ImageSelector {

    private Logger LOG = Logger.getLogger(MultiImageSelector.class);

    public MultiImageSelector(Context context) {
        super(context);
        mName = context.getResources().getString(R.string.content_multi_images);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context, R.drawable.ic_attachment_select_image, true);
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        String[] arrayExtra = intent.getStringArrayExtra(MultiImagePickerActivity.EXTRA_IMAGES);
        if(arrayExtra != null && arrayExtra.length > 0) {
            return true;
        }
        return false;
    }

    @Override
    public Intent createSelectionIntent(Context context) {
        Intent intent = new Intent(context, MultiImagePickerActivity.class);
        return intent;
    }

    @Nullable
    public ArrayList<IContentObject> createObjectsFromSelectionResult(Context context, Intent intent) {
        if(!isValidIntent(context, intent)) {
            return null;
        }
        String[] uris = intent.getStringArrayExtra(MultiImagePickerActivity.EXTRA_IMAGES);
        ArrayList<IContentObject> selected = new ArrayList<IContentObject>();

        for (String uri : uris) {
            IContentCreator creator = findContentObjectCreator(Uri.parse(uri));
            if (creator == null) {
                LOG.warn("No IContentCreator found for url '" + uri + "'");
                return null;
            }
            Intent dataIntent = new Intent();
            dataIntent.setDataAndType(Uri.parse(uri), "image/*");
            SelectedContent selectedContent = creator.apply(context, dataIntent);
            if (selectedContent != null) {
                selected.add(selectedContent);
            }
        }
        return selected;
    }

}
