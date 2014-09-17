package com.hoccer.xo.android.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.android.fragment.QrCodeScannerFragment;
import com.hoccer.xo.release.R;

public class QrCodeActivity extends XoActionbarActivity {
    @Override
    protected int getLayoutResource() {
        return R.layout.default_framelayout;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableUpNavigation();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fl_fragment_container, new QrCodeScannerFragment());
        transaction.commit();
    }
}
