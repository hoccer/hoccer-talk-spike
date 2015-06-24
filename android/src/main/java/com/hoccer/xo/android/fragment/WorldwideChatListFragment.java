package com.hoccer.xo.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.WorldwideController;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.EnvironmentChatListAdapter;
import com.hoccer.xo.android.view.Placeholder;

import static com.hoccer.talk.model.TalkEnvironment.TYPE_WORLDWIDE;

public class WorldwideChatListFragment extends EnvironmentChatListFragment {

    private static final String PREFERENCE_KEY_WORLDWIDE_TUTORIAL_VIEWED = "tutorial_viewed";
    private static final String DIALOG_TAG = "ww_tutorial";

    public WorldwideChatListFragment() {
        mPlaceholder = new Placeholder(R.drawable.placeholder_world, R.string.placeholder_worldwide_text);
    }

    @Override
    public void onResume() {
        super.onResume();
        createAdapter();
    }

    private void displayWorldwideTutorialIfNeeded() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isTutorialViewed = preferences.getBoolean(PREFERENCE_KEY_WORLDWIDE_TUTORIAL_VIEWED, false);
        if(!isTutorialViewed) {
            final DialogFragment dialogFragment = new DialogFragment() {
                @Override
                public Dialog onCreateDialog(Bundle savedInstanceState) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Worldwide");
                    View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_ww_tutorial, null);
                    ListView optionsListView = (ListView) view.findViewById(R.id.lv_dialog_ww_tutorial);
                    optionsListView.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.simple_list_item_centered,
                            getResources().getStringArray(R.array.worldwide_tutorial_options)));
                    optionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            SharedPreferences.Editor editor = preferences.edit();
                            switch(position) {
                                case 0:
                                    editor.putString(getString(R.string.preference_key_worldwide_timetolive), "3600000"); // 1 hour
                                    break;
                                case 1:
                                    editor.putString(getString(R.string.preference_key_worldwide_timetolive), "21600000"); // 6 hours
                                    break;
                                case 2:
                                    editor.putString(getString(R.string.preference_key_worldwide_timetolive), "86400000"); // 24 hours
                                    break;
                            }
                            editor.putBoolean(PREFERENCE_KEY_WORLDWIDE_TUTORIAL_VIEWED, true);
                            editor.commit();
                            dismiss();
                        }
                    });
                    builder.setView(view);
                    return builder.create();
                }
            };
            dialogFragment.show(getActivity().getFragmentManager(), DIALOG_TAG);
        }
    }
    @Override
    public void onDestroy() {
        if (mListAdapter != null) {
            mListAdapter.unregisterListeners();
        }

        destroyAdapter();
        super.onDestroy();
    }

    private void createAdapter() {
        if (mListAdapter == null) {
            mListAdapter = new EnvironmentChatListAdapter(TYPE_WORLDWIDE, mActivity);
            mListAdapter.registerListeners();
            setListAdapter(mListAdapter);
        }
    }

    private void destroyAdapter() {
        if (mListAdapter != null) {
            setListAdapter(null);
            mListAdapter.unregisterListeners();
            mListAdapter = null;
        }
    }

    @Override
    public View getCustomTabView(Context context) {
        return null;
    }

    @Override
    public String getTabName(Resources resources) {
        return resources.getString(R.string.worldwide_tab_name);
    }

    @Override
    public void onPageResume() {}

    @Override
    public void onPageSelected() {
        WorldwideController.get().activateWorldwide();

        TalkClientContact group = XoApplication.get().getXoClient().getCurrentWorldwideGroup();
        mListAdapter.scheduleUpdate(group);
        displayWorldwideTutorialIfNeeded();
    }

    @Override
    public void onPageUnselected() {
        WorldwideController.get().deactivateWorldWide();
    }

    @Override
    public void onPagePause() {}

    @Override
    public void onPageScrollStateChanged(int state) {}
}
