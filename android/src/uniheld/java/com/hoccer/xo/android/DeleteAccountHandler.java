package com.hoccer.xo.android;

import java.io.File;

import static com.hoccer.xo.android.activity.StudentCardActivityFragment.STUDENT_CARD_FILE_NAME;

public class DeleteAccountHandler {
    public static void onDeleteAccount() {
        File file = new File(XoApplication.getAttachmentDirectory(), STUDENT_CARD_FILE_NAME);
        if (file.exists()) {
            file.delete();
        }
    }
}
