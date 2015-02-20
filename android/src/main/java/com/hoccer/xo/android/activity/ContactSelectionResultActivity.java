package com.hoccer.xo.android.activity;

import android.content.Intent;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;

public class ContactSelectionResultActivity extends ContactSelectionActivity implements ContactSelectionFragment.IContactSelectionListener {

    @Override
    protected void handleContactSelection() {
        Intent intent = new Intent();
        intent.putIntegerArrayListExtra(ContactSelectionFragment.EXTRA_SELECTED_CONTACT_IDS, getSelectedContactIdsFromFragment());
        setResult(RESULT_OK, intent);

        finish();
    }
}
