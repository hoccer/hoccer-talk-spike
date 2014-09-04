package com.hoccer.xo.android.fragment;

import android.app.TaskStackBuilder;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AdapterView;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.adapter.NearbyChatAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;


public class NearbyArchiveFragment extends ListFragment {

    static final Logger LOG = Logger.getLogger(NearbyArchiveFragment.class);

    private NearbyChatAdapter mAdapter;

    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            if (mAdapter.getCount() == 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    Intent upIntent = getActivity().getParentActivityIntent();
                    if (upIntent != null) {
                        if (getActivity().shouldUpRecreateTask(upIntent)) {
                            TaskStackBuilder.create(getActivity()).addNextIntentWithParentStack(upIntent).startActivities();
                        } else {
                            getActivity().navigateUpTo(upIntent);
                        }
                    } else {
                        getActivity().onBackPressed();
                    }
                } else {
                    getActivity().onBackPressed();
                }
            }
        }
    };

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setStackFromBottom(true);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view.getTag().equals(TalkClientMessage.TYPE_SEPARATOR)) {
                    final ChatMessageItem chatItem = mAdapter.getItem(position);
                    XoDialogs.showYesNoDialog("ConfirmDeletion",
                            R.string.dialog_confirm_nearby_deletion_title,
                            R.string.dialog_confirm_nearby_deletion_message,
                            getActivity(),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    try {
                                        XoApplication.getXoClient().getDatabase().deleteAllMessagesFromContactId(chatItem.getConversationContactId());
                                        mAdapter.requestReload();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    } finally {
                                        mAdapter.onResume();
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mAdapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                }
                            }, null);
                }
            }
        });

        mAdapter = new NearbyChatAdapter(getListView(), (XoActivity) getActivity());
        mAdapter.onCreate();
        setListAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        getListAdapter().registerDataSetObserver(mDataSetObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getListAdapter().unregisterDataSetObserver(mDataSetObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.onDestroy();
    }
}
