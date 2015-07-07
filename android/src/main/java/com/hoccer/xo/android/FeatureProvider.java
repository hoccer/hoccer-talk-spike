package com.hoccer.xo.android;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.hoccer.xo.android.activity.component.ViewPagerActivityComponent;
import com.hoccer.xo.android.dialog.WorldWideTutorialDialog;
import com.hoccer.xo.android.fragment.WorldwideChatListFragment;

import static com.hoccer.xo.android.dialog.WorldWideTutorialDialog.DIALOG_TAG;

public class FeatureProvider {

    /**
     * Select worldwide tab on first start
     */

    private static final String PREFERENCE_KEY_WORLDWIDE_PAGE_SHOWN_ON_FIRST_START = "worldwide_page_shown_on_first_start";

    public static void selectWorldwidePageOnFirstStart(Context context, ViewPagerActivityComponent viewPagerActivityComponent, WorldwideChatListFragment worldwideChatListFragment) {
        if (isFirstStart(context)) {
            viewPagerActivityComponent.selectFragment(worldwideChatListFragment);

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREFERENCE_KEY_WORLDWIDE_PAGE_SHOWN_ON_FIRST_START, true);
            editor.apply();
        }
    }

    private static boolean isFirstStart(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean worldwidePageShown = preferences.getBoolean(PREFERENCE_KEY_WORLDWIDE_PAGE_SHOWN_ON_FIRST_START, false);

        return !worldwidePageShown;
    }

    /**
     * Show worldwide tutorial on first start
     */

    public static final String PREFERENCE_KEY_WORLDWIDE_TUTORIAL_VIEWED = "worldwide_tutorial_viewed";

    public static void displayWorldwideTutorialIfNeeded(Activity activity) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean isTutorialViewed = preferences.getBoolean(PREFERENCE_KEY_WORLDWIDE_TUTORIAL_VIEWED, false);
        if (!isTutorialViewed) {
            new WorldWideTutorialDialog().show(activity.getFragmentManager(), DIALOG_TAG);
        }
    }
}
