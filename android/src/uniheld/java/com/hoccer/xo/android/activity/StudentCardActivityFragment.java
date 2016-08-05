package com.hoccer.xo.android.activity;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.*;
import android.widget.ImageView;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.util.UriUtils;

import java.io.File;

public class StudentCardActivityFragment extends Fragment {

    private ImageView mStudentCardImageView;

    public StudentCardActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_card, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mStudentCardImageView = (ImageView) view.findViewById(R.id.iv_student_card);

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "student_card.jpg");
        if (file.exists()) {
            updatePicture(file.getPath());
        }
    }

    public void updatePicture(String filePath) {
        mStudentCardImageView.setImageURI(null);
        mStudentCardImageView.setImageURI(Uri.parse(filePath));
    }
}
