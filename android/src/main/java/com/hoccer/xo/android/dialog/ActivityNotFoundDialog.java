package com.hoccer.xo.android.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;


public class ActivityNotFoundDialog extends DialogFragment {

    public static final String ACTION_KEY = "action";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View contentView = createContentView(getArguments().getString(ACTION_KEY));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Application not found");
        builder.setView(contentView);

        return builder.create();
    }

    private View createContentView(String action) {
        TextView textView = new TextView(getActivity());
        textView.setPadding(20, 20, 20, 20);

        if (Intent.ACTION_GET_CONTENT.equals(action)) {
            SpannableString spannable = new SpannableString("No File Browser application available. Please install from http://play.google.com/store/search?q=file%20manager&c=apps");
            Linkify.addLinks(spannable, Linkify.WEB_URLS);
            textView.setText(spannable);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            textView.setText("Application not found.");
        }

        return textView;
    }
}