package com.hoccer.xo.android.activity;

import android.content.Intent;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContactSelectionResultActivity extends ContactSelectionActivity {

    @Override
    protected void handleContactSelection(List<TalkClientContact> selectedContacts) {
        Intent intent = new Intent();
        intent.putIntegerArrayListExtra(ContactSelectionFragment.EXTRA_SELECTED_CONTACT_IDS, transformToContactIds(selectedContacts));
        setResult(RESULT_OK, intent);

        finish();
    }

    private ArrayList<Integer> transformToContactIds(List<TalkClientContact> selectedContacts) {
        ArrayList<Integer> selectedContactIds = new ArrayList<Integer>();
        Collection<Integer> collection = CollectionUtils.collect(selectedContacts, new Transformer<TalkClientContact, Integer>() {
            @Override
            public Integer transform(TalkClientContact contact) {
                return contact.getClientContactId();
            }
        });

        selectedContactIds.addAll(collection);
        return selectedContactIds;
    }
}
