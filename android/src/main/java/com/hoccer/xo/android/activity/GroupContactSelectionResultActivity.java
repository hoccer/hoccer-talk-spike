package com.hoccer.xo.android.activity;

import android.view.Menu;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;

public class GroupContactSelectionResultActivity extends ContactSelectionResultActivity {

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_contact_selection_ok).setVisible(true);
        return true;
    }

    @Override
    public boolean shouldShow(TalkClientContact contact) {
        boolean shouldShow = false;
        if (contact.isClient()) {
            if (contact.isClientFriend() || contact.isInEnvironment() || contact.isKept()) {
                shouldShow = true;
            }
        }

        return shouldShow;
    }

    @Override
    public void onContactSelectionChanged(int count) {}
}
