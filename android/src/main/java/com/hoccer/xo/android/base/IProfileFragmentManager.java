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
     * @param startInActionMode Set to true if the dialog should be shown with action mode enabled
     * @param addToBackStack Set to true to add the fragment transition to the back stack
     */
    void showGroupProfileFragment(int groupContactId, boolean startInActionMode, boolean addToBackStack);

    /**
     * Displays the GroupProfileCreationFragment with a given group id.
     *
     * @param cloneGroupId The ID of the group contact to clone, can be nil to create a new group.
     */
    void showGroupProfileCreationFragment(String cloneGroupId);
}
