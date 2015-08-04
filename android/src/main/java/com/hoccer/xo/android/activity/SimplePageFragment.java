package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.fragment.IPagerFragment;
import org.apache.log4j.Logger;


public class SimplePageFragment extends XoListFragment implements IPagerFragment {

    private static final Logger LOG = Logger.getLogger(SimplePageFragment.class);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        LOG.error("onAttach()");
    }

    @Override
    public void onResume() {
        super.onResume();
        LOG.error("- onResume()");
    }

    @Override
    public void onPageSelected() {
        getActivity().toString();
        LOG.error("-- onPageSelected()");
    }

    @Override
    public void onPageUnselected() {
        getActivity().toString();
        LOG.error("-- onPageUnselected()");
    }

    @Override
    public void onPause() {
        super.onPause();
        LOG.error("- onPause()");
    }

    @Override //never called
    public void onDetach() {
        super.onDetach();
        LOG.error("onDetach()");
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public View getCustomTabView(Context context) {
        return null;
    }

    @Override
    public String getTabName(Resources resources) {
        return "TEST";
    }
}
