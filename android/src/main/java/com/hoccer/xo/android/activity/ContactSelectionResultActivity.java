package com.hoccer.xo.android.activity;

import android.content.Intent;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;

import java.util.List;

import static com.hoccer.talk.client.model.TalkClientContact.transformToContactIds;

public class ContactSelectionResultActivity extends ContactSelectionActivity {

    @Override
    protected void handleContactSelection(List<TalkClientContact> selectedContacts) {
        Intent intent = new Intent();
        intent.putIntegerArrayListExtra(ContactSelectionFragment.EXTRA_SELECTED_CONTACT_IDS, transformToContactIds(selectedContacts));
        setResult(RESULT_OK, intent);

        finish();
    }
}
