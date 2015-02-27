package com.hoccer.talk.util;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

public class ProgressOutputStream extends OutputStream {

    private static final Logger LOG = Logger.getLogger(ProgressOutputStream.class);

    private final int mOffset;

    private int mProgress;

    private final OutputStream mWrapped;

    private final IProgressListener mListener;

    public ProgressOutputStream(OutputStream wrapped, IProgressListener listener, int offset) {
        mWrapped = wrapped;
        mListener = listener;
        mOffset = offset;
    }

    public int getProgress() {
        return mProgress;
    }

    @Override
    public void write(int b) throws IOException {
        mWrapped.write(b);
        mProgress += 1;
        callListener();
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException {
        mWrapped.write(b);
        mProgress += b.length;
        callListener();
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        mWrapped.write(b, off, len);
        mProgress += len;
        callListener();
    }

    private void callListener() {
        LOG.trace("progress " + (mOffset + mProgress));
        if(mListener != null) {
            mListener.onProgress(mOffset + mProgress);
        }
    }

    @Override
    public void flush() throws IOException {
        mWrapped.flush();
    }

    @Override
    public void close() throws IOException {
        mWrapped.close();
    }

}
