package com.hoccer.xo.android.content;

import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.XoApplication;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.UUID;


public class SelectedLocation extends SelectedContent {

    private static final Logger LOG = Logger.getLogger(SelectedLocation.class);

    private final byte[] mData;

    public SelectedLocation(byte[] data) {
        super(null, "application/json", ContentMediaType.LOCATION);
        mData = data;
    }

    @Override
    public String writeContentToFile() {
        File file = new File(XoApplication.getAttachmentDirectory(), UUID.randomUUID().toString());
        try {
            file.createNewFile();
            FileUtils.writeByteArrayToFile(file, mData);
        } catch (IOException e) {
            LOG.error("Could not save location to file", e);
        }

        return file.getPath();
    }
}
