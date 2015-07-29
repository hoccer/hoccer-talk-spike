package com.hoccer.xo.android;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.hoccer.xo.android.dialog.WorldWideTutorialDialog;

import static com.hoccer.xo.android.dialog.WorldWideTutorialDialog.DIALOG_TAG;

public class FeaturePromoter {

    /**
     * Cleanup for selecting worldwide tab on first start
     * <p/>
     * This is for removing an unnecessary shared preference entry used in an earlier version.
     */

    private static final String PREFERENCE_KEY_WORLDWIDE_PAGE_SHOWN_ON_FIRST_START = "worldwide_page_shown_on_first_start";

    public static void cleanupForSelectWorldwidePageOnFirstStart(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(PREFERENCE_KEY_WORLDWIDE_PAGE_SHOWN_ON_FIRST_START).apply();
    }

    /**
     * Show worldwide tutorial on first start
     */

    public static final String PREFERENCE_KEY_WORLDWIDE_TUTORIAL_VIEWED = "worldwide_tutorial_viewed";

    public static void displayWorldwideTutorialOnFirstStart(Activity activity) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean isTutorialViewed = preferences.getBoolean(PREFERENCE_KEY_WORLDWIDE_TUTORIAL_VIEWED, false);
        if (!isTutorialViewed) {
            new WorldWideTutorialDialog().show(activity.getFragmentManager(), DIALOG_TAG);
        }
    }
}
