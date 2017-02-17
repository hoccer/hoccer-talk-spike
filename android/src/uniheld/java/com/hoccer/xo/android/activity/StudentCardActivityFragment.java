package com.hoccer.xo.android.activity;

import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.util.ImageUtils;
import com.hoccer.xo.android.view.Placeholder;
import org.apache.log4j.Logger;

import java.io.File;

public class StudentCardActivityFragment extends Fragment {
    private static final Logger LOG = Logger.getLogger(StudentCardActivityFragment.class);

    private static final Placeholder PLACEHOLDER = new Placeholder(R.drawable.placeholder_student_card, R.string.placeholder_student_card_text);
    public static final String STUDENT_CARD_FILE_NAME = "student_card.jpg";

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
    }

    @Override
    public void onResume() {
        super.onResume();

        File file = new File(XoApplication.getAttachmentDirectory(), STUDENT_CARD_FILE_NAME);

        if (file.exists()) {
            PLACEHOLDER.removeFromView(getView());
            updatePicture(file.getPath());
        } else {
            PLACEHOLDER.applyToView(getView(), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((StudentCardActivity) getActivity()).takePicture();
                }
            });
        }
    }

    private void updatePicture(String filePath) {
        mStudentCardImageView.setImageURI(null);
        mStudentCardImageView.setImageURI(Uri.parse(filePath));
    }
}
