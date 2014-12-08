package com.hoccer.xo.android.activity;

import android.os.Bundle;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.activity.component.ViewPagerActivityComponent;
import com.hoccer.xo.android.fragment.QrCodeGeneratorFragment;
import com.hoccer.xo.android.fragment.QrCodeScannerFragment;
import com.artcom.hoccer.R;

public class QrCodeActivity extends ComposableActivity {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_qr_code;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[] {
                new MediaPlayerActivityComponent(this),
                new ViewPagerActivityComponent(this,
                        R.id.pager,
                        new QrCodeScannerFragment(),
                        new QrCodeGeneratorFragment())
        };
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableUpNavigation();
    }
}
