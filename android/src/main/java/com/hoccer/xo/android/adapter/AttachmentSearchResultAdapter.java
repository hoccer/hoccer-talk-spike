package com.hoccer.xo.android.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.util.UriUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter of attachment search results.
 */
public class AttachmentSearchResultAdapter extends BaseAdapter {

    List<XoTransfer> mItems;
    List<XoTransfer> mMatchedItems;

    private String mLastQuery = "";

    public AttachmentSearchResultAdapter(List<XoTransfer> items) {
        mItems = items;
        mMatchedItems = new ArrayList<XoTransfer>();
    }

    @Override
    public XoTransfer getItem(int position) {
        return mMatchedItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getTransferId();
    }

    @Override
    public int getCount() {
        return mMatchedItems.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createAttachmentView(parent.getContext(), mMatchedItems.get(position));
    }

    public void query(String query) {
        mLastQuery = query.toLowerCase();
        mMatchedItems.clear();
        if (!mLastQuery.isEmpty()) {
            for (XoTransfer attachment : mItems) {
                MediaMetaData metaData = MediaMetaData.retrieveMetaData(UriUtils.getAbsoluteFileUri(attachment.getFilePath()).getPath());
                String title = metaData.getTitle();
                String artist = metaData.getArtist();
                if (title.toLowerCase().contains(mLastQuery) || artist.toLowerCase().contains(mLastQuery)) {
                    mMatchedItems.add(attachment);
                }
            }
        }
        notifyDataSetChanged();
    }

    private View createAttachmentView(Context context, XoTransfer attachment) {
        View attachmentView = View.inflate(context, R.layout.item_attachment_search_result, null);
        String type = attachment.getMediaType();
        if (type.equals(ContentMediaType.AUDIO)) {
            attachmentView = setupAudioAttachmentView(context, attachmentView, attachment);
        }
        return attachmentView;
    }

    private View setupAudioAttachmentView(final Context context, View attachmentView, XoTransfer attachment) {
        TextView titleTextView = (TextView) attachmentView.findViewById(R.id.tv_title);

        MediaMetaData metaData = MediaMetaData.retrieveMetaData(UriUtils.getAbsoluteFileUri(attachment.getFilePath()).getPath());
        String title = metaData.getTitleOrFilename();
        titleTextView.setText(getHighlightedSearchResult(title, mLastQuery));

        TextView artistTextView = (TextView) attachmentView.findViewById(R.id.tv_artist);
        String artist = metaData.getArtist();
        if (artist.isEmpty()) {
            artistTextView.setText(context.getResources().getString(R.string.media_meta_data_unknown_artist));
        } else {
            artistTextView.setText(getHighlightedSearchResult(artist, mLastQuery));
        }

        View artworkContainer = attachmentView.findViewById(R.id.fl_artwork);
        artworkContainer.setVisibility(View.VISIBLE);

        int thumbnailWidth = DisplayUtils.getDisplaySize(context).x / 4;

        final ImageView artworkView = (ImageView) attachmentView.findViewById(R.id.iv_artwork);
        metaData.getResizedArtwork(context.getResources(), new MediaMetaData.ArtworkRetrieverListener() {
            @Override
            public void onArtworkRetrieveFinished(final Drawable artwork) {
                new Handler(context.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        artworkView.setImageDrawable(artwork);
                    }
                });

            }
        }, thumbnailWidth);

        return attachmentView;
    }

    private static Spannable getHighlightedSearchResult(String text, String query) {
        Spannable result = new SpannableString(text);
        String lowerCaseText = text.toLowerCase();

        int fromIndex = 0;
        int highlightStart;

        highlightStart = lowerCaseText.indexOf(query, fromIndex);
        while (highlightStart >= 0) {
            int highlightEnd = highlightStart + query.length();
            result.setSpan(new ForegroundColorSpan(Color.BLACK), highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            fromIndex = highlightEnd;
            highlightStart = lowerCaseText.indexOf(query, fromIndex);
        }

        return result;
    }
}
