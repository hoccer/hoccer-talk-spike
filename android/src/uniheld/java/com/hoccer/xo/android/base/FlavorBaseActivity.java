package com.hoccer.xo.android.base;

import android.content.Intent;
import android.view.MenuItem;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.activity.StudentCardActivity;

public abstract class FlavorBaseActivity extends BaseActivity {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_show_student_card:
                showStudentCard();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showStudentCard() {
        startActivity(new Intent(this, StudentCardActivity.class));
    }
}
