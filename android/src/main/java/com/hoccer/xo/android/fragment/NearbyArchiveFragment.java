package com.hoccer.xo.android.fragment;

import android.app.Activity;
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

/**
 * Created by nico on 08/08/2014.
 */
public class NearbyArchiveFragment extends ListFragment {

    static final Logger LOG = Logger.getLogger(NearbyArchiveFragment.class);

    private AdapterView.OnItemClickListener mItemClickListener;
    private DataSetObserver mDataSetObserver;
    private NearbyChatAdapter mAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mItemClickListener = setupOnItemClickListener();
        mDataSetObserver = setupDataSetObserver(activity);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setStackFromBottom(true);
        getListView().setOnItemClickListener(mItemClickListener);
        mAdapter = new NearbyChatAdapter(getListView(), (XoActivity) getActivity());
        setListAdapter(mAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        getListAdapter().registerDataSetObserver(mDataSetObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getListAdapter().unregisterDataSetObserver(mDataSetObserver);
        mDataSetObserver = null;
        mAdapter = null;
    }

    private AdapterView.OnItemClickListener setupOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
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
        };
    }

    private DataSetObserver setupDataSetObserver(final Activity activity) {
        return new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mAdapter.getCount() == 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        Intent upIntent = activity.getParentActivityIntent();
                        if (upIntent != null) {
                            if (activity.shouldUpRecreateTask(upIntent)) {
                                TaskStackBuilder.create(getActivity()).addNextIntentWithParentStack(upIntent).startActivities();
                            } else {
                                activity.navigateUpTo(upIntent);
                            }
                        } else {
                            activity.onBackPressed();
                        }
                    } else {
                        activity.onBackPressed();
                    }
                }
            }
        };
    }
}
