package com.hoccer.xo.android.base;

public interface IMessagingFragmentManager {

    void showMessageFragment(int contactId);

    void showSingleProfileFragment(int clientContactId);

    void showGroupProfileFragment(int groupContactId);
}