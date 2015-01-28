package com.hoccer.xo.android.content;

import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedAttachment;
import com.hoccer.xo.android.XoApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


public class SelectedLocation extends SelectedAttachment {

    private final byte[] mData;

    public SelectedLocation(byte[] data) {
        mData = data;
    }

    @Override
    protected String writeToFile() {
        File file = new File(XoApplication.getAttachmentDirectory(), UUID.randomUUID().toString());
        try {
            file.createNewFile();
            OutputStream os = new FileOutputStream(file);
            os.write(mData);
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getPath();
    }

    @Override
    public String getContentMediaType() {
        return ContentMediaType.LOCATION;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public double getAspectRatio() {
        return 0.0f;
    }
}
