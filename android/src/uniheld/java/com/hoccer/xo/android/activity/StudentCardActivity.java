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
import org.apache.log4j.Logger;

import java.io.File;

public class StudentCardActivity extends Activity {

    private static final Logger LOG = Logger.getLogger(StudentCardActivity.class);
    private static final int TAKE_PICTURE_REQUEST_CODE = 1;

    private Uri mFileUri;

    private StudentCardActivityFragment mStudentCardFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_card);

        mStudentCardFragment = (StudentCardActivityFragment) getFragmentManager().findFragmentById(R.id.student_card_fragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_student_card, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_take_picture:
                takePicture();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PICTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            String filePath = mFileUri.getPath();
            mStudentCardFragment.updatePicture(filePath);
        }
    }

    private void takePicture() {
        try {
            mFileUri = createOutputMediaFileUri();
        } catch (ExternalStorageNotMountedException e) {
            Toast.makeText(this, "Error accessing public storage directory", Toast.LENGTH_LONG).show();
            LOG.error(e.getLocalizedMessage(), e);
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);

        startActivityForResult(intent, TAKE_PICTURE_REQUEST_CODE);
    }

    private Uri createOutputMediaFileUri() throws ExternalStorageNotMountedException {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "student_card.jpg");
            return Uri.fromFile(file);
        } else {
            throw new ExternalStorageNotMountedException("External storage is not mounted.");
        }
    }
}
