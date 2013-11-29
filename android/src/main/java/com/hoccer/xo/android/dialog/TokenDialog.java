package com.hoccer.xo.android.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class TokenDialog extends SherlockDialogFragment implements DialogInterface.OnClickListener {

    private static final Logger LOG = Logger.getLogger(TokenDialog.class);

    XoActivity mActivity;

    TalkClientSmsToken mToken;

    public TokenDialog (XoActivity activity, TalkClientSmsToken token) {
        super();
        mActivity = activity;
        mToken = token;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LOG.debug("onCreateDialog()");

        // XXX similar code in RichtContactsAdapter.updateToken()
        ContentResolver resolver = mActivity.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(mToken.getSender()));

        String name = mToken.getSender();
        String photo = "content://" + R.drawable.avatar_default_contact;

        Cursor cursor = resolver.query(uri,
                new String[] {
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                },
                null, null, null);

        if(cursor != null && cursor.getCount() > 0) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
            cursor.moveToFirst();
            name = cursor.getString(nameIndex);
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(getSherlockActivity());

        // XXX i18n mess
        builder.setTitle("Invitation");
        builder.setCancelable(true);
        builder.setNegativeButton("Decline", this);
        builder.setPositiveButton("Accept", this);
        builder.setNeutralButton("Cancel", this);

        builder.setMessage(name + " has sent you an invitation via SMS.\nDo you wish to accept?");

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == DialogInterface.BUTTON_POSITIVE) {
            LOG.debug("onClick(accept)");
            if(mToken != null) {
                mActivity.getXoClient().useSmsToken(mToken);
                mActivity.hackReturnedFromDialog();
            }
        }
        if(which == DialogInterface.BUTTON_NEGATIVE) {
            LOG.debug("onClick(decline)");
            if(mToken != null) {
                mActivity.getXoClient().rejectSmsToken(mToken);
                mActivity.hackReturnedFromDialog();
            }
        }
        if(which == DialogInterface.BUTTON_NEUTRAL) {
            LOG.debug("onClick(cancel)");
            dialog.dismiss();
            mActivity.hackReturnedFromDialog();
        }
    }

}
