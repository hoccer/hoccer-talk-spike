package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.xo.android.NearbyController;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.adapter.EnvironmentChatListAdapter;
import com.hoccer.xo.android.view.Placeholder;


public class NearbyChatListFragment extends EnvironmentChatListFragment {

    private EnvironmentChatListAdapter mNearbyAdapter;

    public NearbyChatListFragment() {
        mPlaceholder = new Placeholder(
                R.drawable.placeholder_nearby,
                R.drawable.placeholder_nearby_point,
                R.string.placeholder_nearby_text);
    }

    @Override
    public void onResume() {
        super.onResume();
        createAdapter();
    }

    @Override
    public void onDestroy() {
        if (mNearbyAdapter != null) {
            mNearbyAdapter.unregisterListeners();
        }

        destroyAdapter();
        super.onDestroy();
    }

    private void createAdapter() {
        if (mNearbyAdapter == null) {
            mNearbyAdapter = new EnvironmentChatListAdapter(TalkEnvironment.TYPE_NEARBY, mActivity);
            mNearbyAdapter.registerListeners();
            setListAdapter(mNearbyAdapter);
        }
    }

    private void destroyAdapter() {
        if (mNearbyAdapter != null) {
            setListAdapter(null);
            mNearbyAdapter.unregisterListeners();
            mNearbyAdapter = null;
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

    @Override
    public void onPageSelected() {
    }

    @Override
    public void onPageUnselected() {
        NearbyController.get().disableNearbyMode();
    }

    @Override
    public void onPageResume() {
        if (NearbyController.get().locationServicesEnabled()) {
            NearbyController.get().enableNearbyMode();
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
    public void onPagePause() {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}
