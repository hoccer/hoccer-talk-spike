package com.hoccer.xo.android.view;

import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class AttachmentAudioView extends LinearLayout implements View.OnClickListener{

    private Context mContext;
    private MediaMetaData mItemData;
    private ServiceConnection mConnection;
    private BroadcastReceiver mReceiver;
    private MediaPlayerService mMediaPlayerService;
    private int mPosition;

    private static final Logger LOG = Logger.getLogger(AttachmentAudioView.class);

    public AttachmentAudioView(Context context, int position, MediaMetaData itemData) {
        super(context);
        initialize(context, position, itemData);
    }

    private void initialize(Context context, int position, MediaMetaData itemData) {

        mContext = context;
        mItemData = itemData;
        mPosition = position;

        addView(inflate(mContext, R.layout.attachmentlist_music_item, null));

        Intent intent = new Intent(mContext, MediaPlayerService.class);
        mContext.startService(intent);
        bindService(intent);

        String titleName = mItemData.getTitle(getResources().getString(R.string.app_name));
        String artistName = mItemData.getArtist();

        ((TextView) findViewById(R.id.attachmentlist_item_title_name)).setText(titleName);
        ((TextView) findViewById(R.id.attachmentlist_item_artist_name)).setText(artistName);

        ImageView coverView = ((ImageView) findViewById(R.id.attachmentlist_item_image));

        byte[] cover = mItemData.getArtwork();

        if( cover != null ) {
            Bitmap coverBitmap = BitmapFactory.decodeByteArray(cover, 0, cover.length);
            coverView.setImageBitmap(coverBitmap);
        }
    }

    private void bindService(Intent intent){

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MediaPlayerService.MediaPlayerBinder binder = (MediaPlayerService.MediaPlayerBinder) service;
                mMediaPlayerService = binder.getService();

                updatePlayPauseView();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mMediaPlayerService = null;
            }
        };

        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public boolean isActive() {
        if (isBound()) {
            return !mMediaPlayerService.isPaused() && !mMediaPlayerService.isStopped() && (("file://" + mItemData.getFilePath()).equals(mMediaPlayerService.getCurrentMediaFilePath()));
        } else {
            return false;
        }
    }

    private void updatePlayPauseView(){
        findViewById(R.id.attachmentlist_item_playpause_button).setVisibility((isActive()) ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        Intent intent = new Intent(mContext, MediaPlayerService.class);
        mContext.startService(intent);
        bindService(intent);

        createBroadcastReceiver();
    }

    @Override
    public void onClick(View v) {}

    private void createBroadcastReceiver() {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(MediaPlayerService.PLAYSTATE_CHANGED_ACTION)) {
                    updatePlayPauseView();
                }
            }
        };
        IntentFilter filter = new IntentFilter(MediaPlayerService.PLAYSTATE_CHANGED_ACTION);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, filter);
    }

    public boolean isBound() {
        return mMediaPlayerService != null;
    }
}
