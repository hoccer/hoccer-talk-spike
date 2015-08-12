package com.hoccer.xo.android.profile;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.BackgroundManager;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.selector.ImageSelector;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public abstract class ProfileFragment extends Fragment {

    private static final Logger LOG = Logger.getLogger(ProfileFragment.class);

    public final static int REQUEST_SELECT_AVATAR = 1;
    public final static int REQUEST_CROP_AVATAR = 2;
    public final static String ARG_CLIENT_CONTACT_ID = "ARG_CLIENT_CONTACT_ID";

    protected TalkClientContact mContact;

    protected TextView mNameText;
    protected ImageView mAvatarImage;

    private ImageSelector mAvatarSelection;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContact = getContactById();
    }

    private TalkClientContact getContactById() {
        if (getArguments() != null && getArguments().containsKey(ARG_CLIENT_CONTACT_ID)) {
            try {
                int contactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
                return XoApplication.get().getClient().getDatabase().findContactById(contactId);
            } catch (SQLException e) {
                LOG.error("SQL error while retrieving contact ", e);
            }
        }

        return null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAvatarImage = (ImageView) view.findViewById(R.id.profile_avatar_image);
        mNameText = (TextView) view.findViewById(R.id.tv_profile_name);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        LOG.debug("onActivityResult(" + requestCode + "," + resultCode + ")");
        super.onActivityResult(requestCode, resultCode, intent);

        if (intent == null) {
            return;
        }

        if (requestCode == REQUEST_SELECT_AVATAR) {
            if (mAvatarSelection != null) {
                final Intent finalIntent = intent;
                // defer activity start after application came to foreground and XoApplication.setActiveInBackground() has been reset
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        BackgroundManager.get().ignoreNextBackgroundPhase();
                        startActivityForResult(ImageSelector.createCropIntent(finalIntent.getData()), REQUEST_CROP_AVATAR);
                    }
                });
            }
        } else if (requestCode == REQUEST_CROP_AVATAR) {
            intent = selectedAvatarPreProcessing(intent);
            if (intent != null) {
                try {
                    SelectedContent content = mAvatarSelection.createObjectFromSelectionResult(getActivity(), intent);
                    if (content != null) {
                        LOG.debug("selected avatar " + content.getFilePath());
                        onAvatarSelected(content);
                    }
                } catch (Exception e) {
                    LOG.error("Creating selected avatar failed.", e);
                }
            } else {
                showAvatarSelectionError();
            }
        }
    }

    protected abstract void onAvatarSelected(SelectedContent content);

    private void showAvatarSelectionError() {
        Toast.makeText(getActivity(), R.string.error_avatar_selection, Toast.LENGTH_LONG).show();
    }

    private Intent selectedAvatarPreProcessing(Intent data) {
        String uuid = UUID.randomUUID().toString();
        String filePath = XoApplication.getAvatarDirectory().getPath() + File.separator + uuid
                + ".jpg";
        String croppedImagePath = XoApplication.getAttachmentDirectory().getAbsolutePath()
                + File.separator
                + "tmp_crop";
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(croppedImagePath);
            if (bitmap == null) {
                return null;
            }
            File avatarFile = new File(filePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, new FileOutputStream(avatarFile));
            Uri uri = getImageContentUri(getActivity(), avatarFile);
            data.setData(uri);

            File tmpImage = new File(croppedImagePath);
            if (tmpImage.exists()) {
                tmpImage.delete();
            }

            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    protected void selectAvatar() {
        BackgroundManager.get().ignoreNextBackgroundPhase();

        try {
            mAvatarSelection = new ImageSelector(getActivity());
            Intent intent = mAvatarSelection.createSelectionIntent(getActivity());
            startActivityForResult(intent, REQUEST_SELECT_AVATAR);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.error_compatible_app_unavailable, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
