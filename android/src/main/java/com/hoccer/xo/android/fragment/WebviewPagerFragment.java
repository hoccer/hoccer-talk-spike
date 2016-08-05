package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.base.PagerFragment;
import org.apache.log4j.Logger;

public class WebviewPagerFragment extends PagerFragment {

    protected static final Logger LOG = Logger.getLogger(WebviewPagerFragment.class);

    private WebView webView;

    @Override
    public void onPageUnselected() {
    }

    @Override
    public void onPageSelected() {
    }

    @Override
    public void onResume() {
        super.onResume();

        webView.loadUrl(getArguments().getString("url"));
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
        return getArguments().getString("tabName");
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_webview, container, false);

        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

        webView = (WebView) view.findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                LOG.error(error);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void onBackPressed() {
        webView.goBack();
    }

    public WebView getWebView() {
        return webView;
    }
}
