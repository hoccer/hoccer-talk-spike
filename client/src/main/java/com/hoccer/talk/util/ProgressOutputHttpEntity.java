package com.hoccer.talk.util;

import org.apache.http.entity.InputStreamEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProgressOutputHttpEntity extends InputStreamEntity {

    private final IProgressListener mProgressListener;

    private final long mOffset;

    public ProgressOutputHttpEntity(InputStream istream, long length, IProgressListener listener, long offset) {
        super(istream, length);
        mProgressListener = listener;
        mOffset = offset;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        ProgressOutputStream mProgressStream = new ProgressOutputStream(outstream, mProgressListener, (int)mOffset);
        super.writeTo(mProgressStream);
    }
}
