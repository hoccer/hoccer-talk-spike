package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.NearbyController;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.adapter.EnvironmentChatListAdapter;
import com.hoccer.xo.android.base.FlavorBaseActivity;
import com.hoccer.xo.android.view.Placeholder;

import static com.hoccer.talk.model.TalkEnvironment.TYPE_NEARBY;


public class NearbyChatListFragment extends EnvironmentChatListFragment {

    public NearbyChatListFragment() {
        mPlaceholder = new Placeholder(R.drawable.placeholder_nearby, R.string.placeholder_nearby_text);
    }

    @Override
    public void onPageSelected() {
        activateNearby();
    }

    @Override
    public void onPageUnselected() {
        NearbyController.INSTANCE.disableNearbyMode();
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
            mListAdapter = new EnvironmentChatListAdapter(TYPE_NEARBY, (FlavorBaseActivity) getActivity());
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
        return resources.getString(R.string.nearby_tab_name);
    }

    private void activateNearby() {
        if (NearbyController.INSTANCE.isNearbyEnabled()) {
            return;
        }

        if (NearbyController.INSTANCE.locationServicesEnabled()) {
            NearbyController.INSTANCE.enableNearbyMode();
            createAdapter();
        } else {
            showLocationServiceDialog();
        }
    }

    private void showLocationServiceDialog() {
        XoDialogs.showYesNoDialog("EnableLocationServiceDialog",
                R.string.dialog_enable_location_service_title,
                R.string.dialog_enable_location_service_message,
                getActivity(),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getActivity().startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                }
        );
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}
