package com.hoccer.xo.android.activity;

import android.os.Bundle;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.release.R;

public class NearbyContactsActivity extends XoActivity {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_nearby_contacts;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.fragment_contacts;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
