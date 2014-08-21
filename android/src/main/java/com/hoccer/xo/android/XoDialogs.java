package com.hoccer.xo.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

/**
 * This class contains static helper methods for dialogs.
 */
public class XoDialogs {

    private static final Logger LOG = Logger.getLogger(XoDialogs.class);

    public static void showYesNoDialog(final String tag, final int titleId, final int messageId, final Activity activity, final DialogInterface.OnClickListener yesListener) {
        showYesNoDialog(tag, titleId, messageId, activity, yesListener, null);
    }

    public static void showYesNoDialog(final String tag, final int titleId, final int messageId, final Activity activity, final DialogInterface.OnClickListener yesListener, final DialogInterface.OnClickListener noListener) {
        DialogFragment dialogFragment = new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                LOG.debug("Creating dialog: " + tag);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(titleId);
                builder.setMessage(messageId);
                builder.setPositiveButton(R.string.common_yes, yesListener);

                if(noListener != null) {
                    builder.setNegativeButton(R.string.common_no, noListener);
                } else {
                    builder.setNegativeButton(R.string.common_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    });
                }

                return builder.create();
            }
        };
        dialogFragment.show(activity.getFragmentManager(), tag);
    }

    public static void showOkDialog(final String tag, final int titleId, final int messageId, final Activity activity, final DialogInterface.OnClickListener okListener) {
        DialogFragment dialogFragment = new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                LOG.debug("Creating dialog: " + tag);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(titleId);
                builder.setMessage(messageId);
                builder.setNeutralButton(R.string.common_ok, okListener);
                return builder.create();
            }
        };
        dialogFragment.show(activity.getFragmentManager(), tag);
    }

    // extended onClick listener providing the name field content
    public interface OnTextSubmittedListener {
        public void onClick(DialogInterface dialog, int id, String text);
    }

    public static void showInputPasswordDialog(final String tag, final int titleId, final Activity activity, final OnTextSubmittedListener okListener) {
        showInputPasswordDialog(tag, titleId, activity, okListener, null);
    }

    public static void showInputPasswordDialog(final String tag, final int titleId, final Activity activity, final OnTextSubmittedListener okListener, final DialogInterface.OnClickListener cancelListener) {
        final LinearLayout passwordInputView = (LinearLayout)activity.getLayoutInflater().inflate(R.layout.view_password_input, null);
        final EditText passwordInput = (EditText) passwordInputView.findViewById(R.id.password_input);
        final InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        DialogFragment dialogFragment = new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                LOG.debug("Creating dialog: " + tag);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setView(passwordInputView);
                builder.setTitle(titleId);
                builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        inputMethodManager.hideSoftInputFromWindow(passwordInput.getWindowToken(), 0);
                        okListener.onClick(dialog, id, passwordInput.getText().toString());
                    }
                });

                builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        inputMethodManager.hideSoftInputFromWindow(passwordInput.getWindowToken(), 0);
                        if(cancelListener != null) {
                            cancelListener.onClick(dialog, id);
                        }
                    }
                });

                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        // disable positive button initially
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                });
                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

                // update positive button enabled state
                passwordInput.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (TextUtils.isEmpty(s)) {
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        } else {
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }

                    }
                });
                return alertDialog;
            }
        };
        dialogFragment.show(activity.getFragmentManager(), tag);
    }

    public static void showInputTextDialog(final String tag, final int titleId, final Activity activity, final OnTextSubmittedListener okListener) {
        showInputTextDialog(tag, titleId, -1, activity, okListener, null);
    }

    public static void showInputTextDialog(final String tag, final int titleId, final int messageId, final Activity activity, final OnTextSubmittedListener okListener) {
        showInputTextDialog(tag, titleId, messageId, activity, okListener, null);
    }

    public static void showInputTextDialog(final String tag, final int titleId, final int messageId, final Activity activity, final OnTextSubmittedListener okListener, final DialogInterface.OnClickListener cancelListener) {
        final View textInputView = activity.getLayoutInflater().inflate(R.layout.dialog_create_new_item, null);
        final EditText textInput = (EditText) textInputView.findViewById(R.id.et_input_name);
        final InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        DialogFragment dialogFragment = new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                LOG.debug("Creating dialog: " + tag);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setView(textInputView);
                builder.setTitle(titleId);
                if (messageId > 0) {
                    builder.setMessage(messageId);
                }
                builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        inputMethodManager.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
                        okListener.onClick(dialog, id, textInput.getText().toString());

                    }
                });

                builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        inputMethodManager.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
                        if(cancelListener != null) {
                            cancelListener.onClick(dialog, id);
                        }
                    }
                });

                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        // disable positive button initially
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                });
                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

                // update positive button enabled state
                textInput.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (TextUtils.isEmpty(s)) {
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        } else {
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }

                    }
                });
                return alertDialog;
            }
        };
        dialogFragment.show(activity.getFragmentManager(), tag);
    }

    // extended the onClick listener providing the selected item
    public interface OnSingleSelectionFinishedListener {
        public void onClick(DialogInterface dialog, int id, int selectedItem);
    }

    public static void showSingleChoiceDialog(final String tag, final int titleId, final String[] items, final Activity activity, final OnSingleSelectionFinishedListener listener) {
        DialogFragment dialogFragment = new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                LOG.debug("Creating dialog: " + tag);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(titleId);
                final Integer[] selectedIndex = new Integer[]{0};
                builder.setSingleChoiceItems(items, selectedIndex[0], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        selectedIndex[0] = id;
                    }
                });

                builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onClick(dialog, id, selectedIndex[0]);
                    }
                });
                return builder.create();
            }
        };
        dialogFragment.show(activity.getFragmentManager(), tag);
    }

    // extended the onClick listener providing the selected items
    public interface OnMultiSelectionFinishedListener {
        public void onClick(DialogInterface dialog, int id, boolean[] selectionStates);
    }

    public static void showMultiChoiceDialog(final String tag, final int titleId, final String[] items, final boolean[] initialSelectionStates, final Activity activity, final OnMultiSelectionFinishedListener selectionListener) {
        DialogFragment dialogFragment = new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                LOG.debug("Creating dialog: " + tag);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(titleId);
                final boolean[] selectionStates = initialSelectionStates.clone();
                builder.setMultiChoiceItems(items, initialSelectionStates, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        selectionStates[which] = isChecked;
                    }
                });

                builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        selectionListener.onClick(dialog, id, selectionStates);
                    }
                });
                return builder.create();
            }
        };
        dialogFragment.show(activity.getFragmentManager(), tag);
    }
}
