package com.hoccer.xo.android.activity;

import android.view.Menu;
import com.artcom.hoccer.R;

public class GroupContactSelectionResultActivity extends ContactSelectionResultActivity {

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_contact_selection_ok).setVisible(true);
        return true;
    }

    @Override
    public void onContactSelectionChanged(int count) {}
}
