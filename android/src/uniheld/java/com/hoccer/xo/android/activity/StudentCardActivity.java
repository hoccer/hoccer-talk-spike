package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.Intent;
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
import org.apache.log4j.Logger;

import java.io.File;

public class StudentCardActivity extends Activity {

    private static final Logger LOG = Logger.getLogger(StudentCardActivity.class);

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

        startActivity(intent);
    }

    private Uri createOutputMediaFileUri() throws ExternalStorageNotMountedException {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File file = new File(XoApplication.getAttachmentDirectory(), "student_card.jpg");
            return Uri.fromFile(file);
        } else {
            throw new ExternalStorageNotMountedException("External storage is not mounted.");
        }
    }
}
