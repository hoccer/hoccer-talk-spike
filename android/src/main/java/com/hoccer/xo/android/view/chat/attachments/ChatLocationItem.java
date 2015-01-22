package com.hoccer.xo.android.view.chat.attachments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.model.LatLng;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.chat.ChatMessageItem;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class ChatLocationItem extends ChatMessageItem {

    public ChatLocationItem(Context context, TalkClientMessage message) {
        super(context, message);
    }

    @Override
    public ChatItemType getType() {
        return ChatItemType.ChatItemWithLocation;
    }

    @Override
    protected void configureViewForMessage(View view) {
        super.configureViewForMessage(view);
        configureAttachmentViewForMessage(view);
    }

    @Override
    protected void displayAttachment(final IContentObject contentObject) {
        super.displayAttachment(contentObject);

        // add view lazily
        if (mContentWrapper.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout locationLayout = (RelativeLayout) inflater.inflate(R.layout.content_location, null);
            mContentWrapper.addView(locationLayout);
        }
        TextView locationTextView = (TextView) mContentWrapper.findViewById(R.id.tv_location_description);
        TextView locationTitleView = (TextView) mContentWrapper.findViewById(R.id.tv_location_title);
        ImageButton locationButton = (ImageButton) mContentWrapper.findViewById(R.id.ib_content_location);

        int textColor = (mMessage.isIncoming()) ? mContext.getResources().getColor(R.color.xo_incoming_message_textColor) : mContext.getResources().getColor(R.color.xo_compose_message_textColor);

        locationTextView.setTextColor(textColor);
        locationTitleView.setTextColor(textColor);

        locationButton.setBackgroundDrawable(ColorSchemeManager.getRepaintedAttachmentDrawable(mContext, R.drawable.ic_light_location, mMessage.isIncoming()));

        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (contentObject.isContentAvailable()) {
                    Uri uri;
                    if (contentObject.getContentUrl() != null) {
                        uri = Uri.parse(contentObject.getContentUrl());
                    } else {
                        uri = UriUtils.getAbsoluteFileUri(contentObject.getFilePath());
                    }

                    LatLng location = loadGeoJson(uri);
                    if (location != null) {
                        String label = "Received Location";
                        Uri locationUri = Uri.parse("http://maps.google.com/maps?q=loc:" + location.latitude + "," + location.longitude + " (" + label + ")");
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, locationUri);
                        XoActivity activity = (XoActivity) view.getContext();
                        activity.startExternalActivity(intent);
                    }
                }
            }
        });
    }

    private LatLng loadGeoJson(Uri uri) {
        LatLng result = null;
        try {
            InputStream is = new FileInputStream(uri.getPath());
            ObjectMapper jsonMapper = new ObjectMapper();
            JsonNode json = jsonMapper.readTree(is);
            if (json != null && json.isObject()) {
                LOG.info("parsing location: " + json);
                JsonNode location = json.get("location");
                if (location != null && location.isObject()) {
                    JsonNode type = location.get("type");
                    if ("point".equals(type.asText())) {
                        JsonNode coordinates = location.get("coordinates");
                        if (coordinates.isArray() && coordinates.size() == 2) {
                            JsonNode lat = coordinates.get(0);
                            JsonNode lon = coordinates.get(1);
                            result = new LatLng(lat.asDouble(), lon.asDouble());
                        } else {
                            LOG.error("coordinates not an array of 2");
                        }
                    } else {
                        LOG.error("location is not a point");
                    }
                } else {
                    LOG.error("location node is not an object");
                }
            } else {
                LOG.error("root node is not object");
            }
        } catch (IOException e) {
            LOG.error("error loading geojson", e);
        }
        return result;
    }
}
