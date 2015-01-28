package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import com.hoccer.talk.content.SelectedAttachment;

/**
 * Content selectors allow the user to select content from some source via intents
 */
public interface IContentSelector {

    /** Returns the name of this selector */
    public abstract String getName();

    /** Returns the icon for the specific type of content */
    public abstract Drawable getContentIcon();

    public abstract boolean isValidIntent(Context context, Intent intent);

    /** Creates an intent for content selection */
    public abstract Intent createSelectionIntent(Context context);

    /** Handles the intent result, returning a content object */
    public abstract SelectedAttachment createObjectFromSelectionResult(Context context, Intent intent);
}
