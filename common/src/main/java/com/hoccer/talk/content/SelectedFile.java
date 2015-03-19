package com.hoccer.talk.content;

public class SelectedFile extends SelectedContent {

    private double mAspectRatio;

    public SelectedFile(String filePath, String mimeType, String mediaType) {
        super(filePath, mimeType, mediaType);
    }

    public SelectedFile(String filePath, String mimeType, String mediaType, double aspectRatio) {
        super(filePath, mimeType, mediaType);
        mAspectRatio = aspectRatio;
    }

    @Override
    public double getAspectRatio() {
        return mAspectRatio;
    }
}
