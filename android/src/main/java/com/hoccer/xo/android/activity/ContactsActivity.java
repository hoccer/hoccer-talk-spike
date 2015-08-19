package com.hoccer.xo.android.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.activity.component.ViewPagerActivityComponent;
import com.hoccer.xo.android.fragment.ClientContactListFragment;
import com.hoccer.xo.android.fragment.GroupContactListFragment;

import java.sql.SQLException;

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
                        new ClientContactListFragment(),
                        new GroupContactListFragment())
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableUpNavigation();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_pair:
                showPairing();
                break;
            case R.id.menu_new_group:
                showNewGroup();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}