package com.hoccer.xo.android;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.hoccer.xo.android.activity.component.ViewPagerActivityComponent;
import com.hoccer.xo.android.dialog.WorldWideTutorialDialog;
import com.hoccer.xo.android.fragment.WorldwideChatListFragment;

import static com.hoccer.xo.android.dialog.WorldWideTutorialDialog.DIALOG_TAG;

public class FeaturePromoter {

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
