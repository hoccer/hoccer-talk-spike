package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.fragment.WebviewPagerFragment;

public class ChatsActivity extends ChatsBaseActivity {

    private WebviewPagerFragment mWebViewFragment;

    @Override
    protected void onResume() {
        super.onResume();
        if (!mViewPagerActivityComponent.contains(mWebViewFragment)) {
            mWebViewFragment = new WebviewPagerFragment();

            Bundle bundle = new Bundle();
            bundle.putString("url", "http://www.gmx.de");
            bundle.putString("tabName", "BENEFITS");
            mWebViewFragment.setArguments(bundle);

            mViewPagerActivityComponent.add(mWebViewFragment);
        }
    }

    @Override
    public void onBackPressed() {
        if (mViewPagerActivityComponent.getCurrentFragment() == mWebViewFragment && mWebViewFragment.getWebView().canGoBack()) {
            mWebViewFragment.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_take_picture:
                showStudentCard();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void showStudentCard() {
        startActivity(new Intent(this, StudentCardActivity.class));
    }
}
