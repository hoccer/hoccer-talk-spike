package com.hoccer.xo.android.activity;

import android.content.Intent;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;

public class ContactSelectionResultActivity extends ContactSelectionActivity implements ContactSelectionFragment.IContactSelectionListener {

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[]{new MediaPlayerActivityComponent(this)};
    }

    @Override
    protected void handleContactSelection() {
        createResultAndFinish();
    }

    private void createResultAndFinish() {
        Intent intent = new Intent();
        intent.putIntegerArrayListExtra(ContactSelectionFragment.EXTRA_SELECTED_CONTACT_IDS,
                getSelectedContactIdsFromFragment());
        setResult(RESULT_OK, intent);

        finish();
    }
}
