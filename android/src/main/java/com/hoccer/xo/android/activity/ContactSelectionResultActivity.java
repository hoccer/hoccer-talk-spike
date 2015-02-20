package com.hoccer.xo.android.activity;

import android.content.Intent;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;

import java.util.ArrayList;

public class ContactSelectionResultActivity extends ContactSelectionActivity implements ContactSelectionFragment.IContactSelectionListener {

    @Override
    protected void handleContactSelection(ArrayList<Integer> selectedContactIds) {
        Intent intent = new Intent();
        intent.putIntegerArrayListExtra(ContactSelectionFragment.EXTRA_SELECTED_CONTACT_IDS, selectedContactIds);
        setResult(RESULT_OK, intent);

        finish();
    }
}
