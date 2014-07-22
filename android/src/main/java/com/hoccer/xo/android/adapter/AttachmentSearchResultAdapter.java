package com.hoccer.xo.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.release.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nico on 04/07/2014.
 */
public class AttachmentSearchResultAdapter extends AttachmentListAdapter{

    List<AudioAttachmentItem> mFoundAttachments = new ArrayList<AudioAttachmentItem>();
    private String mLastQuery = "";

    public AttachmentSearchResultAdapter(Activity activity) {
        super(activity);
    }

    @Override
    public AudioAttachmentItem getItem(int position) {
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
            for (AudioAttachmentItem attachment : getAttachmentList()) {
                String title = attachment.getMetaData().getTitle();
                String artist = attachment.getMetaData().getArtist();

                if ((title != null && title.toLowerCase().contains(query.toLowerCase())) ||
                        (artist != null && artist.toLowerCase().contains(query.toLowerCase()))) {
                    mFoundAttachments.add(attachment);
                }
            }
        }
    }

    private View createAttachmentView(Context context, AudioAttachmentItem attachment) {
        View attachmentView = View.inflate(context, R.layout.item_attachment_search_result, null);
        String type = attachment.getContentObject().getContentMediaType();
        if (type.equals(ContentMediaType.AUDIO)) {
            attachmentView = setupAudioAttachmentView(context, attachmentView, attachment);
        }
        return attachmentView;
    }

    private View setupAudioAttachmentView(Context context, View attachmentView, AudioAttachmentItem attachment) {
        TextView titleTv = (TextView) attachmentView.findViewById(R.id.tv_title);
        String title = attachment.getMetaData().getTitleOrFilename(attachment.getFilePath());
        titleTv.setText(getHighlightedSearchResult(title, mLastQuery));

        TextView subtitleTv = (TextView) attachmentView.findViewById(R.id.tv_subtitle);
        String artist = attachment.getMetaData().getArtist();
        if (artist == null || artist.isEmpty()) {
            artist = context.getResources().getString(R.string.media_meta_data_unknown_artist);
            subtitleTv.setText(artist);
        } else {
            subtitleTv.setText(getHighlightedSearchResult(artist, mLastQuery));
        }

        View artworkContainer = attachmentView.findViewById(R.id.fl_artwork);
        artworkContainer.setVisibility(View.VISIBLE);

        final ImageView artworkView = (ImageView) attachmentView.findViewById(R.id.iv_artwork);
        attachment.getMetaData().getArtwork(context.getResources(), new MediaMetaData.ArtworkRetrieverListener() {
            @Override
            public void onArtworkRetrieveFinished(Drawable artwork) {
                artworkView.setImageDrawable(artwork);
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
