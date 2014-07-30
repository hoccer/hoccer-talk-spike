package com.hoccer.xo.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.release.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nico on 04/07/2014.
 */
public class AttachmentSearchResultAdapter extends AttachmentListAdapter{

    List<IContentObject> mFoundAttachments = new ArrayList<IContentObject>();
    private String mLastQuery = "";

    @Override
    public IContentObject getItem(int position) {
        return mFoundAttachments.get(position);
    }

    @Override
    public int getCount() {
        return mFoundAttachments.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createAttachmentView(parent.getContext(), mFoundAttachments.get(position));
    }

    public void searchForAttachments(String query) {
        mLastQuery = query.toLowerCase();
        mFoundAttachments.clear();
        if (!mLastQuery.isEmpty()) {
            for (IContentObject attachment : getAttachmentItems()) {
                MediaMetaData metaData = MediaMetaData.retrieveMetaData(attachment.getContentDataUrl());
                String title = metaData.getTitle();
                String artist = metaData.getArtist();

                if ((title != null && title.toLowerCase().contains(query.toLowerCase())) ||
                        (artist != null && artist.toLowerCase().contains(query.toLowerCase()))) {
                    mFoundAttachments.add(attachment);
                }
            }
        }
        notifyDataSetChanged();
    }

    private View createAttachmentView(Context context, IContentObject attachment) {
        View attachmentView = View.inflate(context, R.layout.item_attachment_search_result, null);
        String type = attachment.getContentMediaType();
        if (type.equals(ContentMediaType.AUDIO)) {
            attachmentView = setupAudioAttachmentView(context, attachmentView, attachment);
        }
        return attachmentView;
    }

    private View setupAudioAttachmentView(final Context context, View attachmentView, IContentObject attachment) {
        TextView titleTv = (TextView) attachmentView.findViewById(R.id.tv_title);
        MediaMetaData metaData = MediaMetaData.retrieveMetaData(attachment.getContentDataUrl());
        String title = metaData.getTitleOrFilename();
        titleTv.setText(getHighlightedSearchResult(title, mLastQuery));

        TextView subtitleTv = (TextView) attachmentView.findViewById(R.id.tv_subtitle);
        String artist = metaData.getArtist();
        if (artist == null || artist.isEmpty()) {
            artist = context.getResources().getString(R.string.media_meta_data_unknown_artist);
            subtitleTv.setText(artist);
        } else {
            subtitleTv.setText(getHighlightedSearchResult(artist, mLastQuery));
        }

        View artworkContainer = attachmentView.findViewById(R.id.fl_artwork);
        artworkContainer.setVisibility(View.VISIBLE);

        final ImageView artworkView = (ImageView) attachmentView.findViewById(R.id.iv_artwork);
        metaData.getArtwork(context.getResources(), new MediaMetaData.ArtworkRetrieverListener() {
            @Override
            public void onArtworkRetrieveFinished(final Drawable artwork) {
                new Handler(context.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        artworkView.setImageDrawable(artwork);
                    }
                });

            }
        });

        return attachmentView;
    }

    private Spannable getHighlightedSearchResult(String text, String query) {
        Spannable result = new SpannableString(text);
        String lowerCaseText = text.toLowerCase();

        int fromIndex = 0;
        int highlightStart = 0;

        highlightStart = lowerCaseText.indexOf(query, fromIndex);
        while(highlightStart >= 0) {
            int highlightEnd = highlightStart + query.length();
            result.setSpan(new ForegroundColorSpan(Color.BLACK), highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            fromIndex = highlightEnd;
            highlightStart = lowerCaseText.indexOf(query, fromIndex);
        }

        return result;
    }
}
