package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.ExternalStorageNotMountedException;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.util.ImageUtils;
import org.apache.log4j.Logger;

import java.io.File;

import static com.hoccer.xo.android.activity.StudentCardActivityFragment.STUDENT_CARD_FILE_NAME;

public class StudentCardActivity extends Activity {

    private static final Logger LOG = Logger.getLogger(StudentCardActivity.class);
    private static final int CAPTURE_IMAGE_REQUEST_CODE = 0;

    private Uri mFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_card);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_card, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_take_picture:
                takePicture();
                break;
            case R.id.action_show_student_card_faq:
                showStudentCardFaq();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showStudentCardFaq() {
        Intent intent = new Intent(this, FaqTutorialActivity.class);
        intent.putExtra("URL", getString(R.string.link_faq));
        startActivity(intent);
    }

    public void takePicture() {
        try {
            mFileUri = createOutputMediaFileUri();
        } catch (ExternalStorageNotMountedException e) {
            Toast.makeText(this, "Error accessing public storage directory", Toast.LENGTH_LONG).show();
            LOG.error(e.getLocalizedMessage(), e);
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);

        startActivityForResult(intent, CAPTURE_IMAGE_REQUEST_CODE);
    }

    private Uri createOutputMediaFileUri() throws ExternalStorageNotMountedException {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File file = new File(XoApplication.getAttachmentDirectory(), STUDENT_CARD_FILE_NAME);
            return Uri.fromFile(file);
        } else {
            throw new ExternalStorageNotMountedException("External storage is not mounted.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CAPTURE_IMAGE_REQUEST_CODE) {
            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mFileUri.getPath(), options);
            Point displaySize = DisplayUtils.getDisplaySize(this);
            Bitmap bitmap = ImageUtils.correctRotationAndResize(mFileUri.getPath(), displaySize.x , displaySize.y);
            ImageUtils.compressBitmapToFile(bitmap, new File(mFileUri.getPath()), 90, Bitmap.CompressFormat.JPEG);

        }
    }
}
