package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.os.Bundle;
import com.artcom.hoccer.R;


public class LegalImprintActivity extends Activity {

    public static final String EXTRA_DISPLAY_MODE = "xo_legal_imprint_display_mode";
    public static final String SHOW_ABOUT = "ABOUT";
    public static final String SHOW_LICENSES = "LICENSES";
    public static final String SHOW_EULA = "EULA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            String displayMode = intentExtras.getString(EXTRA_DISPLAY_MODE);

            if (SHOW_ABOUT.equals(displayMode)) {
                setContentView(R.layout.activity_about);
            } else if (SHOW_LICENSES.equals(displayMode)) {
                setContentView(R.layout.activity_licenses);
            } else if (SHOW_EULA.equals(displayMode)) {
                setContentView(R.layout.eula);
            }
        }
    }
}
