package com.hoccer.talk.util;

import org.apache.http.entity.InputStreamEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProgressOutputHttpEntity extends InputStreamEntity {

    private final IProgressListener mProgressListener;

    private final int mOffset;

    private ProgressOutputStream mProgressStream = null;

    public ProgressOutputHttpEntity(InputStream istream, int length, IProgressListener listener, int offset) {
        super(istream, length);
        mProgressListener = listener;
        mOffset = offset;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        mProgressStream = new ProgressOutputStream(outstream, mProgressListener, mOffset);
        super.writeTo(mProgressStream);
    }

}
