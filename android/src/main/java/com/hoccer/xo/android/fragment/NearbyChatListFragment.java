package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.NearbyController;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.adapter.NearbyChatListAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.view.Placeholder;


public class NearbyChatListFragment extends XoListFragment implements IPagerFragment {

    private static final Placeholder PLACEHOLDER = new Placeholder(
            R.drawable.placeholder_nearby,
            R.drawable.placeholder_nearby_point,
            R.string.placeholder_nearby_text);

    private NearbyChatListAdapter mNearbyAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        PLACEHOLDER.applyToView(view);
        return view;
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
            mNearbyAdapter = new NearbyChatListAdapter(getXoDatabase(), getXoActivity());
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
    public void onListItemClick(ListView listView, View v, int position, long id) {
        super.onListItemClick(listView, v, position, id);
        Object item = listView.getItemAtPosition(position);
        if (item instanceof TalkClientContact) {
            TalkClientContact contact = (TalkClientContact) item;
            getXoActivity().showContactConversation(contact);
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
    public void onPageSelected() {}

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
    public void onPagePause() {}

    @Override
    public void onPageScrollStateChanged(int state) {}
}
