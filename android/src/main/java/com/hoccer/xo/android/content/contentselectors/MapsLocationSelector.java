package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.activity.MapsLocationActivity;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.release.R;

public class MapsLocationSelector implements IContentSelector {

    private String mName;
    private Drawable mIcon;

    public MapsLocationSelector(Context context) {
        mName = context.getResources().getString(R.string.content_location);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context.getResources(), R.drawable.ic_attachment_select_location, true);
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
    public Intent createSelectionIntent(Context context) {
        return new Intent(context, MapsLocationActivity.class);
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {

        boolean isValidIntent = isValidIntent(context, intent);
        if (!isValidIntent) {
            return null;
        }

        SelectedContent content = null;
        if(intent.hasExtra(MapsLocationActivity.EXTRA_GEOJSON)) {
            String json = intent.getStringExtra(MapsLocationActivity.EXTRA_GEOJSON);
            content = new SelectedContent(json.getBytes());
            content.setContentMediaType(ContentMediaType.LOCATION);
            content.setContentType("application/json");
        }
        return content;
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        return (intent.hasExtra(MapsLocationActivity.EXTRA_GEOJSON));
    }

}
