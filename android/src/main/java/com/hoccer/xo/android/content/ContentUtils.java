package com.hoccer.xo.android.content;

public class ContentUtils {

    public static boolean isMimeTypeContact(String mimeType) {
        return mimeType != null && mimeType.startsWith("vnd.android.cursor.item/contact");
    }

    public static boolean isMimeTypeAudio(String mimeType) {
        return mimeType != null && (mimeType.startsWith("audio/") || mimeType.startsWith("application/ogg"));
    }

    public static boolean isMimeTypeVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public static boolean isMimeTypeImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }
}
