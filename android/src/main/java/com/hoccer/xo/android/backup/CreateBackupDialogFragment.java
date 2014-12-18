package com.hoccer.xo.android.backup;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.XoApplication;

import java.io.File;

public class CreateBackupDialogFragment extends DialogFragment {

    private AlertDialog mDialog;

    public interface CreateBackupDialogListener {
        public void onDialogPositiveClick(String password, boolean includeAttachments);
    }

    private CreateBackupDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_backup, null);

        final TextView saveBackupTextView = (TextView) view.findViewById(R.id.tv_save_backup_target);
        final EditText passwordInput = (EditText) view.findViewById(R.id.et_password);
        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.cb_include_attachments);

        String backupDirPath = XoApplication.getAttachmentDirectory().getName() + File.separator + XoApplication.getBackupDirectory().getName();
        String text = getString(R.string.create_backup_dialog_message, backupDirPath);
        saveBackupTextView.setText(text);

        passwordInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Button button = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);

                int length = passwordInput.getText().length();
                if (length > 0) {
                    button.setEnabled(true);
                } else {
                    button.setEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });

        builder.setView(view)
                .setNegativeButton(R.string.common_cancel, null)
                .setTitle(R.string.create_backup);

        builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mListener != null && passwordInput.getText().length() > 0) {
                    mListener.onDialogPositiveClick(passwordInput.getText().toString(), checkBox.isChecked());
                }
            }
        });

        mDialog = builder.create();

        mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });

        return mDialog;
    }

    public void setListener(CreateBackupDialogListener listener) {
        mListener = listener;
    }
}
