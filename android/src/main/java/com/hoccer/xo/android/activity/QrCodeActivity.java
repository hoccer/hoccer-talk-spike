package com.hoccer.xo.android.activity;

import android.os.Bundle;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.release.R;

public class QrCodeActivity extends XoActionbarActivity {
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_qr_code;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableUpNavigation();
    }
}
