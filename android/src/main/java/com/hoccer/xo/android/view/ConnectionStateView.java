package com.hoccer.xo.android.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.xo.android.XoApplication;


public class ConnectionStateView extends LinearLayout implements IXoStateListener {

    private final Handler mHandler = new Handler();

    private TextView mConnectionStateTextView;

    public ConnectionStateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    private void initializeView() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_connection_state, this, true);
        mConnectionStateTextView = (TextView) this.findViewById(R.id.connection_state_text);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        XoApplication.get().getXoClient().registerStateListener(this);
        updateConnectionStateView();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        XoApplication.get().getXoClient().unregisterStateListener(this);
    }

    private void updateConnectionStateView() {
        int connectionState = XoApplication.get().getXoClient().getState();

        switch (connectionState) {
            case XoClient.STATE_DISCONNECTED:
                this.setVisibility(View.VISIBLE);
                mConnectionStateTextView.setText(R.string.connection_state_disconnected);
                break;
            case XoClient.STATE_CONNECTING:
                this.setVisibility(View.VISIBLE);
                mConnectionStateTextView.setText(R.string.connection_state_disconnected);
                break;
            case XoClient.STATE_LOGIN:
            this.setVisibility(View.VISIBLE);
                mConnectionStateTextView.setText(R.string.connection_state_login);
                break;
            case XoClient.STATE_SYNCING:
                this.setVisibility(View.VISIBLE);
                mConnectionStateTextView.setText(R.string.connection_state_synching);
                break;
            case XoClient.STATE_READY:
                this.setVisibility(View.GONE);
                break;
            case XoClient.STATE_REGISTERING:
                this.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onClientStateChange(XoClient client, int state) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateConnectionStateView();
            }
        });
    }
}

