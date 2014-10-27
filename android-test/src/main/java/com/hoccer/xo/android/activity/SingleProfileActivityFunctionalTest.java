package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.fragment.SingleProfileCreationFragment;
import org.apache.log4j.Logger;


public class SingleProfileActivityFunctionalTest extends ActivityInstrumentationTestCase2<SingleProfileActivity> {

    private static final Logger LOG = Logger.getLogger(SingleProfileActivity.class);

    private SingleProfileActivity activity;
    private SingleProfileCreationFragment singleProfileCreationFragment;

    public SingleProfileActivityFunctionalTest() {
        super(SingleProfileActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        Intent intent = new Intent();
        intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CREATE_SELF, true);
        setActivityIntent(intent);

        activity = getActivity();
        singleProfileCreationFragment = (SingleProfileCreationFragment) getActivity().getSupportFragmentManager().
                findFragmentByTag(SingleProfileActivity.SINGLE_PROFILE_CREATION_FRAGMENT);
    }

    public void testPreconditions() {
        assertTrue(activity.getIntent().hasExtra(SingleProfileActivity.EXTRA_CLIENT_CREATE_SELF));
        assertNotNull(singleProfileCreationFragment);
        assertTrue(singleProfileCreationFragment.isAdded());
        assertTrue(singleProfileCreationFragment.isVisible());
    }

    public void testActionModeUI() {
        TextView nameText = (TextView) singleProfileCreationFragment.getView().findViewById(com.hoccer.xo.release.R.id.tv_profile_name);
        EditText editName = (EditText) singleProfileCreationFragment.getView().findViewById(com.hoccer.xo.release.R.id.et_profile_name);
        RelativeLayout keyContainer = (RelativeLayout) singleProfileCreationFragment.getView().findViewById(com.hoccer.xo.release.R.id.inc_profile_key);

        assertEquals(keyContainer.getVisibility(), View.GONE);
        assertEquals(nameText.getVisibility(), View.INVISIBLE);
        assertEquals(editName.getVisibility(), View.VISIBLE);
        assertEquals(editName.getHint(), activity.getString(com.hoccer.xo.release.R.string.profile_name_hint));
        assertEquals(editName.getText().toString(), "");
    }

    @UiThreadTest
    public void testOnDestroyActionMode() {
        final EditText editName = (EditText) singleProfileCreationFragment.getView().findViewById(com.hoccer.xo.release.R.id.et_profile_name);
        final String expectedName = "myName";
        editName.setText(expectedName);

        int closeButtonId = Resources.getSystem().getIdentifier("action_mode_close_button", "id", "android");
        final View closeButton = getActivity().findViewById(closeButtonId);
        closeButton.performClick();

        assertEquals(expectedName, XoApplication.getXoClient().getSelfContact().getSelf().getRegistrationName());
        assertTrue(activity.isFinishing());
    }

    @UiThreadTest
    public void testOnDestroyActionModeWithoutName() {
        final String expectedName = getActivity().getString(com.hoccer.xo.release.R.string.profile_self_initial_name);

        int closeButtonId = Resources.getSystem().getIdentifier("action_mode_close_button", "id", "android");
        final View closeButton = getActivity().findViewById(closeButtonId);
        closeButton.performClick();

        assertEquals(expectedName, XoApplication.getXoClient().getSelfContact().getSelf().getRegistrationName());
        assertTrue(activity.isFinishing());
    }
}
