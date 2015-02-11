package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class FaqTutorialActivity extends Activity {

    private WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebView = new WebView(this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(FaqTutorialActivity.this, description, Toast.LENGTH_SHORT).show();
            }
        });
        mWebView.loadUrl(getIntent().getStringExtra("URL"));
        setContentView(mWebView);
    }
}
