package com.hoccer.xo.android.backup;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;

import static com.hoccer.xo.android.backup.BackupController.OnCancelListener;

public class BackupPreference extends Preference {

    private View mView;
    private final String mInProgressText;

    private OnCancelListener mCancelListener;
    private boolean mInProgress;

    public BackupPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        int inProgressTextId = attrs.getAttributeResourceValue(null, "inProgressText", -1);

        if (inProgressTextId == -1) {
            throw new IllegalArgumentException("Missing inProgressText attribute");
        }

        mInProgressText = getContext().getString(inProgressTextId);
    }

    public void setCancelListener(OnCancelListener cancelListener) {
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
            final TextView inProgressText = (TextView) mView.findViewById(R.id.tv_in_progress);

            inProgressText.setText(mInProgressText);
            defaultLayout.setVisibility(View.GONE);
            inProgressLayout.setVisibility(View.VISIBLE);

            Button cancelBtn = (Button) mView.findViewById(R.id.btn_cancel);
            if (mCancelListener == null) {
                cancelBtn.setVisibility(View.GONE);
            } else {
                cancelBtn.setVisibility(View.VISIBLE);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Button cancelButton = (Button) v;
                        cancelButton.setEnabled(false);
                        inProgressText.setText(R.string.progress_cancelling);
                        mCancelListener.onCancel();
                    }
                });
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
