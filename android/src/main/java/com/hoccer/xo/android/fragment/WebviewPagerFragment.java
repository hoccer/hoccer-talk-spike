package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.PagerFragment;
import com.hoccer.xo.android.view.Placeholder;
import org.apache.log4j.Logger;

import java.io.File;

public class WebviewPagerFragment extends PagerFragment  {

    protected static final Logger LOG = Logger.getLogger(WebviewPagerFragment.class);

    private static final Placeholder PLACEHOLDER = new Placeholder(R.drawable.placeholder_benefits, R.string.placeholder_benefits_offline);
    private static final int REQUEST_UPLOAD_IMAGE = 1;
    private final ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
    private  ConnectivityManager connectivityManager;
    private ValueCallback<Uri[]> filePathCallback;
    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private String url;
    private File outputFile;

    private class ConnectivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo activeNetworkInfo =  ((ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            handleConnectivityChange(activeNetworkInfo);
        }

        private void handleConnectivityChange(NetworkInfo activeNetworkInfo) {
            if (activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting()) {
                applyPlaceholder();
            } else {
                webView.loadUrl(url);
            }
        }
    }

    @Override
    public void onPageUnselected() { }

    @Override
    public void onPageSelected() { }

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
        connectivityManager = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        getContext().registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        View view = inflater.inflate(R.layout.fragment_webview, container, false);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

        webView = (WebView) view.findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");
                WebviewPagerFragment.this.filePathCallback = filePathCallback;

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Upload");
                outputFile = new File(XoApplication.getAttachmentDirectory(),"uniheldID.jpg");

                Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                camIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputFile));
                Intent[] intentArray = {camIntent};
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, REQUEST_UPLOAD_IMAGE);
                return true;

            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                swipeRefreshLayout.setRefreshing(true);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
            }

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
                view.loadUrl("about:blank");
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(getString(R.string.link_benefits))) {
                    return false;
                }

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

        applyPlaceholder();
        url = getArguments().getString("url");
        webView.loadUrl(url);
        return view;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri[] results = null;
        if (requestCode == REQUEST_UPLOAD_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                results = new Uri[]{Uri.fromFile(outputFile)};
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                } else {
                    results = new Uri[]{Uri.fromFile(outputFile)};
                }
            }
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    public WebView getWebView() {
        return webView;
    }

    private void applyPlaceholder(){
        swipeRefreshLayout.setEnabled(false);
        if (getView() != null) {
            PLACEHOLDER.applyToView(getView(), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                }
            });
        }
    }

    private void removePlaceholder() {
        swipeRefreshLayout.setEnabled(true);
        if (getView() != null) {
            PLACEHOLDER.removeFromView(getView());
        }
    }

    private boolean isConnected(){
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}
