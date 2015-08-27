package com.hoccer.xo.android.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.WorldwideController;

import static com.hoccer.xo.android.FeaturePromoter.PREFERENCE_KEY_WORLDWIDE_TUTORIAL_VIEWED;

public class WorldWideTutorialDialog extends DialogFragment {

    public static final String DIALOG_TAG = "worldwide_tutorial";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.worldwide_capitalized);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_ww_tutorial, null);
        ListView optionsListView = (ListView) view.findViewById(R.id.lv_dialog_ww_tutorial);
        optionsListView.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.simple_list_item_centered,
                getResources().getStringArray(R.array.worldwide_tutorial_options)));
        optionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = preferences.edit();
                String timeToLive = "0";
                switch (position) {
                    case 0:
                        timeToLive = "0"; // 1 hour
                        break;
                    case 1:
                        timeToLive = "3600000"; // 1 hour
                        break;
                    case 2:
                        timeToLive = "21600000"; // 6 hour
                        break;
                    case 3:
                        timeToLive = "86400000"; // 24 hour
                        break;
                }
                editor.putString(getString(R.string.preference_key_worldwide_timetolive), timeToLive);
                editor.putBoolean(PREFERENCE_KEY_WORLDWIDE_TUTORIAL_VIEWED, true);
                editor.apply();
                WorldwideController.INSTANCE.updateWorldwideEnvironmentParameters();
                dismiss();
            }
        });
        builder.setView(view);
        return builder.create();
    }
}
