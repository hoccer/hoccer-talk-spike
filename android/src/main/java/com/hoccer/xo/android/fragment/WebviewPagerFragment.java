package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.ProgressBar;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.PagerFragment;
import com.hoccer.xo.android.view.Placeholder;
import org.apache.log4j.Logger;


public class WebviewPagerFragment extends PagerFragment implements IXoStateListener {

    protected static final Logger LOG = Logger.getLogger(WebviewPagerFragment.class);

    private static final Placeholder PLACEHOLDER = new Placeholder(R.drawable.placeholder_benefits, R.string.placeholder_benefits_offline);
    private final Handler handler = new Handler();

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onPageUnselected() {
    }

    @Override
    public void onPageSelected() {
        updateConnectionStateView(XoApplication.get().getClient().getState());
    }

    @Override
    public void onResume() {
        super.onResume();
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

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar_webView);

        webView = (WebView) view.findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                showProgress(newProgress);
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshLayout.setRefreshing(false);
                if (isConnected()) {
                    removePlaceholder();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                LOG.error(error);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("mailto:")) {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                    return true;
                } else if (url.startsWith("http:") || url.startsWith("https:")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        webView.loadUrl(getArguments().getString("url"));

        XoApplication.get().getClient().registerStateListener(this);

        return view;
    }

    public WebView getWebView() {
        return webView;
    }

    @Override
    public void onClientStateChange(XoClient client) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateConnectionState(XoApplication.get().getClient().getState());
            }
        });
    }

    private void updateConnectionState(XoClient.State connectionState) {
        switch (connectionState) {
            case DISCONNECTED:
                applyPlaceholder();
                break;
            case CONNECTING:
                webView.reload();
                break;
        }
    }

    private void applyPlaceholder(){
        swipeRefreshLayout.setEnabled(false);
        if (getView() != null) {
            PLACEHOLDER.applyToView(getView(), new View.OnClickListener() {
                @Override
                public void onClick(View v) { }
            });
        }
    }

    private void removePlaceholder() {
        swipeRefreshLayout.setEnabled(true);
        if (getView() != null) {
            PLACEHOLDER.removeFromView(getView());
        }
    }

    private void showProgress(final int progress) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(progress < 100);
                progressBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
                progressBar.setProgress(progress);
            }
        });
    }

    private boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}
