package com.hoccer.xo.android.backup;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
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
    private EditText mPasswordInput;
    private CheckBox mCheckBox;

    public interface CreateBackupDialogListener {
        public void onDialogPositiveClick(String password, boolean includeAttachments);
    }

    private CreateBackupDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDialog = buildDialog(createView());
        mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                disableOkButton((AlertDialog) dialog);
            }
        });
        return mDialog;
    }

    private View createView() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_create_backup, null);
        TextView saveBackupTextView = (TextView) view.findViewById(R.id.tv_save_backup_target);

        mCheckBox = (CheckBox) view.findViewById(R.id.cb_include_attachments);
        mPasswordInput = (EditText) view.findViewById(R.id.et_password);

        saveBackupTextView.setText(Html.fromHtml(getBackupPathInfo()));
        mPasswordInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Button button = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                int length = mPasswordInput.getText().length();
                button.setEnabled(length > 0);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        return view;
    }

    private AlertDialog buildDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setNegativeButton(R.string.common_cancel, null)
                .setTitle(R.string.backup_title);
        builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mListener != null) {
                    mListener.onDialogPositiveClick(mPasswordInput.getText().toString(), mCheckBox.isChecked());
                }
            }
        });
        return builder.create();
    }

    private String getBackupPathInfo() {
        String path = XoApplication.getAttachmentDirectory().getName() + File.separator + XoApplication.getBackupDirectory().getName();
        return getString(R.string.backup_dialog_message, path);
    }

    public void setListener(CreateBackupDialogListener listener) {
        mListener = listener;
    }

    private static void disableOkButton(AlertDialog dialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }
}
