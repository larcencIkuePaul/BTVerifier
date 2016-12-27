package com.sanjetco.btverifier;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by PaulLee on 11/15/2016.
 * Web view activity
 */
public class ViewWeb extends Activity {

    WebView mWebview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebview = new WebView(this);
        setContentView(mWebview);
        mWebview.getSettings().setJavaScriptEnabled(true);

        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }
        });

        mWebview.loadUrl("https://www.pushcam.com/portal");
    }
}
