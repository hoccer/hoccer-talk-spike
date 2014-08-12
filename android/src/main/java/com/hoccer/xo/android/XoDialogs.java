package com.hoccer.xo.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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
                builder.setTitle(titleId);
                builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        inputMethodManager.hideSoftInputFromWindow(passwordInput.getWindowToken(), 0);
                        okListener.onClick(dialog, id, passwordInput.getText().toString());
                    }
                });

                if(cancelListener != null) {
                    builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            inputMethodManager.hideSoftInputFromWindow(passwordInput.getWindowToken(), 0);
                            cancelListener.onClick(dialog, id);
                        }
                    });
                }
                builder.setView(passwordInputView);
                Dialog dialog = builder.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                return dialog;
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

                if(cancelListener != null) {
                    builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            inputMethodManager.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
                            cancelListener.onClick(dialog, id);
                        }
                    });
                }
                builder.setView(textInputView);
                Dialog dialog = builder.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                return dialog;
            }
        };
        dialogFragment.show(activity.getFragmentManager(), tag);
    }

    // Used to set an integer value from within an anonymous method.
    private static class IntegerBox {
        public IntegerBox(int value) {
            this.value = value;
        }
        public int value;
    }

    public static void showSingleChoiceDialog(final String tag, final int titleId, final int items, final Activity activity, final DialogInterface.OnClickListener okListener) {
        DialogFragment dialogFragment = new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                LOG.debug("Creating dialog: " + tag);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(titleId);
                final IntegerBox selectedIndex = new IntegerBox(0);
                builder.setSingleChoiceItems(items, selectedIndex.value, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        selectedIndex.value = id;
                    }
                });

                builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        okListener.onClick(dialog, selectedIndex.value);
                    }
                });
                return builder.create();
            }
        };
        dialogFragment.show(activity.getFragmentManager(), tag);
    }
}
