package com.hoccer.xo.android.base;


public interface IProfileFragmentManager {

    /**
     * Displays the SingleProfileFragment with a given client contact id.
     *
     * @param clientContactId The client contact id of the contact to display
     */
    void showSingleProfileFragment(int clientContactId);

    /**
     * Displays the GroupProfileFragment with a given client contact id.
     *
     * @param groupContactId The contact id of the group contact to display
     * @param isFollowUp     Set to true if the dialog is displayed after a GroupProfileCreationFragment.
     */
    void showGroupProfileFragment(int groupContactId, boolean isFollowUp);

    /**
     * Displays the GroupProfileCreationFragment with a given client contact id.
     *
     * @param groupContactId The contact id of the group contact to display, can be nil to create a new group.
     * @param cloneProfile   Set to true if the group from the given groupContactId should be cloned.
     */
    void showGroupProfileCreationFragment(int groupContactId, boolean cloneProfile);
}
