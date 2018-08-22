package com.hoccer.xo.android.eulaprompt;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.artcom.hoccer.R;

public class EulaPromptFragment extends Fragment {
    public static final String ARG_HINT_STRING_ID = "ARG_HINT_STRING_ID";

    EulaPromptListener mListener;
    private Button mAcceptButton;
    private Button mDeclineButton;

    public interface EulaPromptListener {
        void onEulaAccepted();
        void onEulaDeclined();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (EulaPromptListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_eula_prompt, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAcceptButton = (Button) view.findViewById(R.id.btn_accept_eula);
        mAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onEulaAccepted();
            }
        });
        mDeclineButton = (Button) view.findViewById(R.id.btn_decline_eula);
        mDeclineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onEulaDeclined();
            }
        });
    }

}

