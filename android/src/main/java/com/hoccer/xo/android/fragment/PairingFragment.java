package com.hoccer.xo.android.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hoccer.talk.client.IXoPairingListener;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.DeviceContactsSelectionActivity;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.release.R;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PairingFragment extends XoFragment implements View.OnClickListener, IXoPairingListener {

    TextView mTokenMessage;
    TextView mTokenText;
    Button mTokenSendSms;
    Button mTokenSendEmail;

    EditText mTokenEdit;
    Button mTokenPairButton;

    Button mQrShowButton;
    Button mQrScanButton;

    ScheduledFuture<?> mTokenFuture;

    TextWatcher mTextWatcher;

    String mActiveToken;
    String mTokenFromEmail;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            mTokenFromEmail = uri.toString();
        }
        LOG.debug("onCreate()");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LOG.debug("onCreateView()");

        View view = inflater.inflate(R.layout.fragment_pairing, container, false);

        mTokenMessage = (TextView) view.findViewById(R.id.pairing_token_message);
        mTokenText = (TextView) view.findViewById(R.id.pairing_token_text);
        mTokenText.setVisibility(View.GONE);
        mTokenSendSms = (Button) view.findViewById(R.id.pairing_token_sms);
        mTokenSendSms.setEnabled(false);
        mTokenSendSms.setOnClickListener(this);

        mTokenSendEmail = (Button) view.findViewById(R.id.pairing_token_email);
        mTokenSendEmail.setEnabled(false);
        mTokenSendEmail.setOnClickListener(this);

        mTokenEdit = (EditText) view.findViewById(R.id.pairing_token_edit);
        mTokenEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        mTokenPairButton = (Button) view.findViewById(R.id.pairing_token_pair);

        mQrScanButton = (Button) view.findViewById(R.id.pairing_scan_qr);
        mQrScanButton.setOnClickListener(this);

        mQrShowButton = (Button) view.findViewById(R.id.pairing_show_qr);
        mQrShowButton.setOnClickListener(this);

        getXoClient().registerPairingListener(this);

        return view;
    }

    @Override
    public void onResume() {
        LOG.debug("onResume()");
        super.onResume();

        requestNewToken();

        // bind the listener for the button that starts pairing
        mTokenPairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performPairing(mTokenEdit.getText().toString());
            }
        });

        // reset the token editor
        mTokenEdit.setText("");
        mTokenPairButton.setEnabled(false);
        // perform pairing on "done" action
        mTokenEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    performPairing(mTokenEdit.getText().toString());
                }
                return false;
            }
        });
        // set up a simple text watcher on the editor
        // so it can disable the pairing button if empty
        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String token = s.toString();
                if (token.length() > 0) {
                    mTokenPairButton.setEnabled(true);
                } else {
                    mTokenPairButton.setEnabled(false);
                }
            }
        };
        mTokenEdit.addTextChangedListener(mTextWatcher);
        if (mTokenFromEmail != null && !mTokenFromEmail.equals("")) {
            performPairing(mTokenFromEmail);
        }
    }

    @Override
    public void onPause() {
        LOG.debug("onPause()");
        // remove the text watcher
        if (mTextWatcher != null) {
            mTokenEdit.removeTextChangedListener(mTextWatcher);
            mTextWatcher = null;
        }
        // cancel token requests
        if (mTokenFuture != null) {
            mTokenFuture.cancel(true);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getXoClient().unregisterPairingListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mQrShowButton) {
            LOG.debug("onClick(qrShow)");
            getXoActivity().showBarcode();
        }
        if (v == mQrScanButton) {
            LOG.debug("onClick(qrScan)");
            getXoActivity().scanBarcode();
        }
        if (v == mTokenSendSms) {
            LOG.debug("onClick(smsSend)");
            Intent addressBook = new Intent(getActivity(), DeviceContactsSelectionActivity.class);
            addressBook.putExtra(DeviceContactsSelectionFragment.EXTRA_IS_SMS_INVITATION, true);
            addressBook.putExtra(DeviceContactsSelectionFragment.EXTRA_TOKEN, mTokenText.getText().toString());
            getActivity().startActivity(addressBook);
        }
        if (v == mTokenSendEmail) {
            LOG.debug("onClick(smsSend)");
            Intent addressBook = new Intent(getActivity(), DeviceContactsSelectionActivity.class);
            addressBook.putExtra(DeviceContactsSelectionFragment.EXTRA_IS_SMS_INVITATION, false);
            addressBook.putExtra(DeviceContactsSelectionFragment.EXTRA_TOKEN, mTokenText.getText().toString());
            getActivity().startActivity(addressBook);
        }
    }

    public void requestNewToken() {
        LOG.debug("requesting new pairing token");
        mTokenText.setVisibility(View.GONE);
        mTokenMessage.setVisibility(View.VISIBLE);
        mTokenSendSms.setEnabled(false);
        mTokenSendEmail.setEnabled(false);
        XoApplication.getExecutor().schedule(new Runnable() {
            @Override
            public void run() {
                final String token = getXoClient().generatePairingToken();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTokenText.setText(token);
                        mTokenText.setVisibility(View.VISIBLE);
                        mTokenMessage.setVisibility(View.GONE);
                        mTokenSendSms.setEnabled(true);
                        mTokenSendEmail.setEnabled(true);
                    }
                });
            }
        }, 1, TimeUnit.SECONDS);
    }

    public void initializeWithReceivedToken(String token) {
        LOG.debug("initializeWithReceivedToken(" + token + ")");
        mTokenEdit.setText(token);
    }

    void performPairing(final String token) {
        LOG.debug("performPairing(" + token + ")");
        mActiveToken = token;
        mTokenEdit.setEnabled(false);
        mTokenPairButton.setEnabled(false);
        getXoActivity().getBackgroundExecutor().execute(new Runnable() {
            @Override
            public void run() {
                getXoClient().performTokenPairing(token);
            }
        });
    }

    @Override
    public void onTokenPairingSucceeded(String token) {
        LOG.debug("onTokenPairingSucceeded()");
        if (token.equals(mActiveToken)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getXoActivity(), R.string.pairing_success, Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }
            });
        }
    }

    @Override
    public void onTokenPairingFailed(String token) {
        LOG.debug("onTokenPairingFailed()");
        if (token.equals(mActiveToken)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showPairingFailure();
                    mTokenEdit.setEnabled(true);
                    mTokenEdit.setText("");
                    mTokenPairButton.setEnabled(false);
                }
            });
        }
    }

    private void showPairingFailure() {
        XoDialogs.showOkDialog("PairingFailedDialog",
                R.string.dialog_pairing_failed_title,
                R.string.dialog_pairing_failed_message,
                getXoActivity(),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int index) {
                    }
                });
    }
}
