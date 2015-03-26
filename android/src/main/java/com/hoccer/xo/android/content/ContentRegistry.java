package com.hoccer.xo.android.content;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.BackgroundManager;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.content.selector.*;
import com.hoccer.xo.android.util.IntentHelper;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Content registry
 * <p/>
 * This singleton is responsible for attachment and avatar selection and viewing.
 * <p/>
 * Essentially, this keeps a registry of selectors and viewers and provides
 * some frontend methods for using them.
 */
public class ContentRegistry {

    private static final Logger LOG = Logger.getLogger(ContentRegistry.class);

    private static ContentRegistry sInstance;

    /* Keys for use with the internal SimpleAdapter in attachment selection */
    private static final String KEY_ICON = "icon";
    private static final String KEY_NAME = "name";
    private static final String KEY_INTENT = "intent";
    private static final String KEY_SELECTOR = "selector";

    /**
     * Get the content registry singleton
     * <p/>
     * The given context will be used to initialize the registry
     * if there isn't one already.
     * <p/>
     * The given context MUST be the application context.
     *
     * @param applicationContext to work in
     * @return the content registry
     */
    public static synchronized ContentRegistry get(Context applicationContext) {
        if (sInstance == null) {
            sInstance = new ContentRegistry(applicationContext);
        }
        return sInstance;
    }

    /**
     * Context for this registry
     */
    Context mContext;

    /**
     * The avatar selector (usually a GallerySelector)
     */
    IContentSelector mAvatarSelector;

    /**
     * Active attachment selectors (only the supported ones)
     */
    List<IContentSelector> mAttachmentSelectors = new ArrayList<IContentSelector>();

    private ClipboardSelector mClipboardSelector;

    private ContentRegistry(Context context) {
        mContext = context;
        initialize();
    }

    /**
     * Initialize the registry
     * <p/>
     * This methods activates supported content selectors and viewers.
     */
    private void initialize() {
        mAvatarSelector = new ImageSelector(mContext);

        initializeSelector(new ImageSelector(mContext));
        initializeSelector(new MultiImageSelector(mContext));
        initializeSelector(new VideoSelector(mContext));
        initializeSelector(new AudioSelector(mContext));
        initializeSelector(new ContactSelector(mContext));
        initializeSelector(new LocationSelector(mContext));
        initializeSelector(new CaptureSelector(mContext));

        mClipboardSelector = new ClipboardSelector(mContext);
    }

    /**
     * Check if the given selector is supported on this device
     * <p/>
     * Adds the selector to the active list when supported.
     *
     * @param selector to add if supported
     */
    private void initializeSelector(IContentSelector selector) {
        Intent intent = selector.createSelectionIntent(mContext);
        if (IntentHelper.isIntentResolvable(intent, mContext)) {
            LOG.debug("content selector " + selector.getName() + " / " + selector.getClass().getSimpleName() + " activated");
            mAttachmentSelectors.add(selector);
        } else {
            LOG.warn("content selector " + selector.getName() + " / " + selector.getClass().getSimpleName() + " not supported");
        }
    }

    public static String getContentDescription(XoTransfer transfer) {
        String mediaTypeString = "Unknown file";
        String mediaType = transfer.getMediaType();
        if (ContentMediaType.IMAGE.equals(mediaType)) {
            mediaTypeString = "Image";
        } else if (ContentMediaType.AUDIO.equals(mediaType)) {
            mediaTypeString = "Audio";
        } else if (ContentMediaType.VIDEO.equals(mediaType)) {
            mediaTypeString = "Video";
        } else if (ContentMediaType.VCARD.equals(mediaType)) {
            mediaTypeString = "Contact";
        } else if (ContentMediaType.LOCATION.equals(mediaType)) {
            mediaTypeString = "Location";
        } else if (ContentMediaType.DATA.equals(mediaType)) {
            mediaTypeString = "Data";
        }

        String sizeString = "";
        if (transfer.getContentLength() > 0) {
            sizeString = " — " + humanReadableByteCount(transfer.getContentLength(), true);
        } else if (transfer.getTransferLength() > 0) {
            sizeString = " — " + humanReadableByteCount(transfer.getTransferLength(), true);
        }

        return mediaTypeString + sizeString;
    }

    /**
     * Starts avatar selection
     * <p/>
     * This will jump directly to the Android gallery.
     *
     * @param activity    that is requesting the selection
     * @param requestCode identifying returned intents
     * @return a new selection handle object
     */
    public ContentSelection selectAvatar(Activity activity, int requestCode) {
        ContentSelection cs = new ContentSelection(activity, mAvatarSelector);
        Intent intent = mAvatarSelector.createSelectionIntent(activity);
        XoActivity xoActivity = (XoActivity) activity;
        xoActivity.startExternalActivityForResult(intent, requestCode);
        return cs;
    }

    /**
     * Create a content object from an intent returned by content selection
     * <p/>
     * Activities should call this when they receive results with the request
     * code they associate with avatar selection (as given to selectAvatar).
     *
     * @param selection handle for the in-progress avatar selection
     * @param intent    returned from the selector
     * @return content object for selected avatar
     */
    public static SelectedContent createSelectedAvatar(ContentSelection selection, Intent intent) throws Exception {
        return selection.getSelector().createObjectFromSelectionResult(selection.getActivity(), intent);
    }

    /**
     * Starts content selection
     * <p/>
     * Will create and show a dialog above the given activity
     * that allows the users to select a source for content selection.
     *
     * @param fragment    that is requesting the selection
     * @param requestCode identifying returned intents
     * @return a new selection handle object
     */
    public ContentSelection selectAttachment(final Fragment fragment, final int requestCode) {
        // create handle representing this selection attempt
        final ContentSelection contentSelection = new ContentSelection(fragment.getActivity());

        // collect selection intents and associated information
        final List<Map<String, Object>> options = new ArrayList<Map<String, Object>>();
        for (IContentSelector selector : mAttachmentSelectors) {
            Map<String, Object> fields = createDataObjectFromContentSelector(fragment.getActivity(), selector);
            if (fields != null) {
                options.add(fields);
            }
        }

        // Add ClipboardSelector when it has something to process
        if (mClipboardSelector.hasContent()) {
            Map<String, Object> fields = createDataObjectFromContentSelector(fragment.getActivity(), mClipboardSelector);
            if (fields != null) {
                options.add(fields);
            }
        }

        // prepare an adapter for the selection options
        SimpleAdapter adapter = new SimpleAdapter(fragment.getActivity(), options, R.layout.select_content,
                new String[]{KEY_ICON, KEY_NAME},
                new int[]{R.id.select_content_icon, R.id.select_content_text});
        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view instanceof ImageView) {
                    ImageView image = (ImageView) view.findViewById(R.id.select_content_icon);
                    image.setImageDrawable((Drawable) data);
                    return true;
                }
                return false;
            }
        });

        // build the selection dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle(R.string.selectattachment_title);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                Map<String, Object> sel = options.get(index);
                IContentSelector selector = (IContentSelector) sel.get(KEY_SELECTOR);
                contentSelection.setSelector(selector);
                Intent intent = (Intent) sel.get(KEY_INTENT);

                if (intent == null) {
                    // selectors without intent can return the result immediately
                    fragment.onActivityResult(requestCode, Activity.RESULT_OK, null);
                } else {
                    startExternalActivityForResult(fragment, intent, requestCode);
                }
            }
        });

        // configure the dialog
        Dialog dialog = builder.create();

        // and show it
        dialog.show();

        // return the selection handle
        return contentSelection;
    }

    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Creates a dialog entry data object from a given IContentSelector.
     *
     * @param context  that is requesting the selection
     * @param selector the given IContentSelector
     * @return a Map containing all relevant intent information
     */
    private static Map<String, Object> createDataObjectFromContentSelector(final Context context, final IContentSelector selector) {
        Intent selectionIntent = selector.createSelectionIntent(context);

        if (selectionIntent == null || IntentHelper.isIntentResolvable(selectionIntent, context)) {
            Map<String, Object> fields = new HashMap<String, Object>();
            fields.put(KEY_INTENT, selectionIntent);
            fields.put(KEY_SELECTOR, selector);
            fields.put(KEY_ICON, selector.getContentIcon());
            fields.put(KEY_NAME, selector.getName());
            return fields;
        }

        return null;
    }

    private static void startExternalActivityForResult(Fragment fragment, Intent intent, int requestCode) {
        XoActivity xoActivity = (XoActivity) fragment.getActivity();

        if (!xoActivity.canStartActivity(intent)) {
            return;
        }

        BackgroundManager.get().ignoreNextBackgroundPhase();
        try {
            fragment.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(xoActivity, R.string.error_compatible_app_unavailable, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
