package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.activity.NearbyContactsActivity;
import com.hoccer.xo.android.adapter.ChatAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.view.OverscrollListView;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;


public class NearbyChatFragment extends XoListFragment implements IXoContactListener {
    private static final Logger LOG = Logger.getLogger(NearbyChatFragment.class);

    private TalkClientContact mCurrentNearbyGroup;

    private ChatAdapter mNearbyAdapter;
    private OverscrollListView mList;
	private ImageView mPlaceholderImageFrame;
    private ImageView mPlaceholderImage;
    private TextView mPlaceholderText;
    private TextView mUserCountText;
    private View mNearbyInfoContainer;
    private CompositionFragment mCompositionFragment;
    private RelativeLayout mCompositionFragmentContainer;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby_chat, container, false);
        mList = (OverscrollListView) view.findViewById(android.R.id.list);
		mPlaceholderImageFrame = (ImageView) view.findViewById(R.id.iv_nearby_placeholder_frame);
        mPlaceholderImageFrame.setImageResource(R.drawable.placeholder_nearby);
        mPlaceholderImage = (ImageView) view.findViewById(R.id.iv_nearby_placeholder);
        mPlaceholderImage.setBackgroundDrawable(ColorSchemeManager.getRepaintedDrawable(getXoActivity(), R.drawable.placeholder_nearby_point, true));
        mPlaceholderText = (TextView) view.findViewById(R.id.tv_nearby_placeholder);
        mPlaceholderText.setText(R.string.placeholder_nearby_text);
        mUserCountText = (TextView) view.findViewById(R.id.tv_nearby_usercount);
        mUserCountText.setText(mActivity.getResources().getString(R.string.nearby_info_usercount, 0));

        mCompositionFragmentContainer = (RelativeLayout) view.findViewById(R.id.fragment_container);
        mCompositionFragment = new CompositionFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, mCompositionFragment).commit();

        mNearbyInfoContainer = view.findViewById(R.id.rl_nearby_info);
        mNearbyInfoContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mActivity, NearbyContactsActivity.class);
                mActivity.startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        getXoActivity().getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCurrentNearbyGroup == null) {
            if (isNearbyConversationPossible()) {
                activateNearbyChat();
            } else {
                deactivateNearbyChat();
            }
        }
        getXoActivity().getXoClient().registerContactListener(this);
    }

    @Override
    public void onDestroy() {
        getXoActivity().getXoClient().unregisterContactListener(this);
        if (mNearbyAdapter != null) {
            mNearbyAdapter.onDestroy();
        }
        super.onDestroy();
    }

    private boolean isNearbyConversationPossible() {
        try {
            final List<TalkClientContact> allNearbyContacts = getXoDatabase().findAllNearbyContacts();
            return (allNearbyContacts.size() > 0);
        } catch (SQLException e) {
            LOG.error("SQL Exception while retrieving current nearby group: ", e);
        }
        return false;
    }

    private void createAdapter(TalkClientContact nearbyGroup) {
        if (nearbyGroup != null) {
            mNearbyAdapter = new ChatAdapter(mList, getXoActivity(), nearbyGroup);
            mNearbyAdapter.onCreate();
            mCompositionFragment.updateContact(nearbyGroup);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mList.setAdapter(mNearbyAdapter);
                }
            });
        }
    }

    private void destroyAdapter() {
        if (mNearbyAdapter != null) {
            mNearbyAdapter.onDestroy();
        }
    }

    public void showPlaceholder() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlaceholderImageFrame.setVisibility(View.VISIBLE);
                mPlaceholderImage.setVisibility(View.VISIBLE);
                mPlaceholderText.setVisibility(View.VISIBLE);
                mList.setVisibility(View.GONE);
                mNearbyInfoContainer.setVisibility(View.GONE);
                mCompositionFragmentContainer.setVisibility(View.GONE);
            }
        });
    }

    private void hidePlaceholder() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlaceholderImageFrame.setVisibility(View.GONE);
                mPlaceholderImage.setVisibility(View.GONE);
                mPlaceholderText.setVisibility(View.GONE);
                mList.setVisibility(View.VISIBLE);
                mNearbyInfoContainer.setVisibility(View.VISIBLE);
                mCompositionFragmentContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void activateNearbyChat() {
        mCurrentNearbyGroup = getXoActivity().getXoClient().getCurrentNearbyGroup();
        createAdapter(mCurrentNearbyGroup);
        hidePlaceholder();
    }

    private void deactivateNearbyChat() {
        showPlaceholder();
        destroyAdapter();
        mCurrentNearbyGroup = null;
    }

    private void updateViews() {
        if (mNearbyAdapter != null) {
            mList.post(new Runnable() {
                @Override
                public void run() {
                    mNearbyAdapter.notifyDataSetChanged();
                    try {
                        final List<TalkClientContact> allNearbyContacts = getXoDatabase().findAllNearbyContacts();
                        mUserCountText.setText(mActivity.getResources().getString(R.string.nearby_info_usercount, allNearbyContacts.size() - 1));
                    } catch (SQLException e) {
                        LOG.error("SQL Exception while retrieving current nearby contacts: ", e);
                    }
                }
            });
        }
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        updateViews();
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        updateViews();
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) { // others
        updateViews();
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        updateViews();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) { // enter first
        if (contact.isGroup()) {
            if (mCurrentNearbyGroup == null || mCurrentNearbyGroup != contact) {
                if (isNearbyConversationPossible()) {
                    activateNearbyChat();
                    updateViews();

                }
            }
        }
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) { // enter - second
        updateViews();
        if (!isNearbyConversationPossible()) {
            deactivateNearbyChat();
        }
    }


}
