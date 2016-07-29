package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.FeaturePromoter;
import com.hoccer.xo.android.WorldwideController;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.EnvironmentChatListAdapter;
import com.hoccer.xo.android.base.BaseActivity;
import com.hoccer.xo.android.base.PagerFragment;
import com.hoccer.xo.android.base.PagerListFragment;
import com.hoccer.xo.android.view.Placeholder;
import org.apache.log4j.Logger;

import static com.hoccer.talk.model.TalkEnvironment.TYPE_WORLDWIDE;

public class WebviewFragment extends PagerFragment {

    protected static final Logger LOG = Logger.getLogger(WebviewFragment.class);

    private WebView webView;

    public WebviewFragment() {
        super();
    }

    @Override
    public void onPageSelected() {
        webView.loadUrl("http://faq.hoccer.com/");
    }

    @Override
    public void onPageUnselected() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View getCustomTabView(Context context) {
        return null;
    }

    @Override
    public String getTabName(Resources resources) {
        return "BENEFITS";
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        webView = new WebView(getContext());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                LOG.error(error);
            }
        });
        return webView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
