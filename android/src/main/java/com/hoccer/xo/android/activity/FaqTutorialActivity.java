package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class FaqTutorialActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(FaqTutorialActivity.this, description, Toast.LENGTH_SHORT).show();
            }
        });
        webView.loadUrl(getIntent().getStringExtra("URL"));
        setContentView(webView);
    }
}
