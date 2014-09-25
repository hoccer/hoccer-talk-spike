package com.hoccer.xo.android.activity;

import android.os.Bundle;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.release.R;

public class DeviceContactsInvitationActivity extends ComposableActivity {

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[] { new MediaPlayerActivityComponent(this) };
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_device_contacts_selection;
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
