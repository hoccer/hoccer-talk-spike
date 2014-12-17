package com.hoccer.xo.android.backup;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;

public class BackupPreference extends Preference {

    private View mView;
    private final String mInProgressText;

    private View.OnClickListener mCancelListener;
    private boolean mInProgress;

    public BackupPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        int inProgressTextId = attrs.getAttributeResourceValue(null, "inProgressText", -1);

        if (inProgressTextId == -1) {
            throw new IllegalArgumentException("Missing inProgressText attribute");
        }

        mInProgressText = getContext().getString(inProgressTextId);
    }

    public void setCancelListener(View.OnClickListener cancelListener) {
        mCancelListener = cancelListener;
    }

    public void setInProgress(boolean inProgress) {
        mInProgress = inProgress;
        updateView();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mView = view;
        updateView();
    }

    private void updateView() {
        if (mView != null) {
            if (mInProgress) {
                showInProgressView();
            } else {
                showDefaultView();
            }
        }
    }

    private void showInProgressView() {
        if (mView != null) {
            RelativeLayout defaultLayout = (RelativeLayout) mView.findViewById(R.id.rl_default_preference);
            RelativeLayout inProgressLayout = (RelativeLayout) mView.findViewById(R.id.rl_in_progress);
            TextView inProgressText = (TextView) mView.findViewById(R.id.tv_in_progress);

            inProgressText.setText(mInProgressText);
            defaultLayout.setVisibility(View.GONE);
            inProgressLayout.setVisibility(View.VISIBLE);

            Button cancelBtn = (Button) mView.findViewById(R.id.btn_cancel);
            if (mCancelListener == null) {
                cancelBtn.setVisibility(View.GONE);
            } else {
                cancelBtn.setVisibility(View.VISIBLE);
                cancelBtn.setOnClickListener(mCancelListener);
            }
        }
    }

    protected void showDefaultView() {
        RelativeLayout defaultLayout = (RelativeLayout) mView.findViewById(R.id.rl_default_preference);
        RelativeLayout inProgressLayout = (RelativeLayout) mView.findViewById(R.id.rl_in_progress);
        defaultLayout.setVisibility(View.VISIBLE);
        inProgressLayout.setVisibility(View.GONE);
    }
}
