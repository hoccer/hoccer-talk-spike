package com.hoccer.xo.android.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.content.selector.MultiImageSelector;
import com.hoccer.xo.android.content.selector.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentSelectionDialogFragment extends DialogFragment {

    public static final String DIALOG_TAG = "AttachmentSelectionDialog";

    private static final String ICON = "icon";
    private static final String NAME = "name";
    private static final String CONTENT_SELECTOR = "selector";

    private OnAttachmentSelectedListener callback;

    public interface OnAttachmentSelectedListener {
        public void onSelected(IContentSelector contentSelector);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            callback = (OnAttachmentSelectedListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("Calling Fragment must implement OnAttachmentSelectedListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<Map<String, Object>> data = createDataForSelectionAdapter();
        SimpleAdapter adapter = createSelectionAdapter(data);
        return createSelectionDialog(data, adapter);
    }

    private List<Map<String, Object>> createDataForSelectionAdapter() {
        final List<Map<String, Object>> options = new ArrayList<Map<String, Object>>();
        for (IContentSelector selector : createContentSelectors()) {
            Map<String, Object> fields = new HashMap<String, Object>();
            fields.put(ICON, selector.getContentIcon());
            fields.put(NAME, selector.getName());
            fields.put(CONTENT_SELECTOR, selector);
            options.add(fields);
        }
        return options;
    }

    private List<IContentSelector> createContentSelectors() {
        List<IContentSelector> contentSelectors = new ArrayList<IContentSelector>();
        contentSelectors.add(new ImageSelector(getActivity()));
        contentSelectors.add(new MultiImageSelector(getActivity()));
        contentSelectors.add(new VideoSelector(getActivity()));
        contentSelectors.add(new AudioSelector(getActivity()));
        contentSelectors.add(new ContactSelector(getActivity()));
        contentSelectors.add(new LocationSelector(getActivity()));
        contentSelectors.add(new CaptureSelector(getActivity()));
        contentSelectors.add(new FileSelector(getActivity()));
        if (Clipboard.get().hasContent()) {
            contentSelectors.add(new ClipboardSelector(getActivity()));
        }

        return contentSelectors;
    }

    private SimpleAdapter createSelectionAdapter(List<Map<String, Object>> options) {
        SimpleAdapter adapter = new SimpleAdapter(getActivity(), options, R.layout.select_content,
                new String[]{ICON, NAME}, new int[]{R.id.select_content_icon, R.id.select_content_text});
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
        return adapter;
    }

    private Dialog createSelectionDialog(final List<Map<String, Object>> options, SimpleAdapter adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                        Map<String, Object> option = options.get(index);
                        callback.onSelected((IContentSelector) option.get(CONTENT_SELECTOR));

                        dialog.dismiss();
                    }
                }
        );

        return builder.create();
    }
}
