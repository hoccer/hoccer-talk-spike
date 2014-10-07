package com.hoccer.xo.android.activity;

import android.os.Bundle;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.activity.component.ViewPagerActivityComponent;
import com.hoccer.xo.android.fragment.ClientListFragment;
import com.hoccer.xo.android.fragment.GroupListFragment;
import com.hoccer.xo.release.R;

public class ContactsActivity extends ComposableActivity {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_contacts;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.menu_activity_contacts;
    }

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[]{
                new MediaPlayerActivityComponent(this),
                new ViewPagerActivityComponent(this,
                        R.id.pager,
                        new ClientListFragment(),
                        new GroupListFragment())
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableUpNavigation();
    }
}
