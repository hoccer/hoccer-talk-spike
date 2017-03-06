package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.PagerFragment;
import com.hoccer.xo.android.view.Placeholder;
import org.apache.log4j.Logger;


public class WebviewPagerFragment extends PagerFragment implements IXoStateListener {

    private static final Placeholder PLACEHOLDER = new Placeholder(R.drawable.placeholder_student_card, R.string.placeholder_student_card_text);
    private final Handler handler = new Handler();

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
        if (XoApplication.get().getClient().isDisconnected()) {
            PLACEHOLDER.applyToView(getView(), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
        } else {
            PLACEHOLDER.removeFromView(getView());
        }
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
                updateConnectionStateView();
            }
        });
    }

    private void updateConnectionStateView() {
        XoClient.State connectionState = XoApplication.get().getClient().getState();

        switch (connectionState) {
            case DISCONNECTED:
                PLACEHOLDER.applyToView(getView(), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
                break;
            case CONNECTING:
                webView.reload();
                PLACEHOLDER.removeFromView(getView());
                break;
        }
    }

}
