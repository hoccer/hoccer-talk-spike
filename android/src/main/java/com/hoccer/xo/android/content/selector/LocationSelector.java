package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.activity.MapsLocationActivity;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ColorSchemeManager;

public class LocationSelector implements IContentSelector {

    private final String mName;
    private final Drawable mIcon;

    public LocationSelector(Context context) {
        mName = context.getResources().getString(R.string.content_location);
        mIcon = ColorSchemeManager.getInkedDrawableCached(R.drawable.ic_attachment_select_location, R.color.primary);
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
        if (intent.hasExtra(MapsLocationActivity.EXTRA_GEOJSON)) {
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
