package com.alnuwaq.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://alnuwaq.com";
    private static final String CART_URL = "https://alnuwaq.com/cart";
    private static final String TRACK_URL = "https://track.alnuwaq.com";
    private static final String TABLES_URL = "https://alnuwaq.com/%D8%B7%D8%A7%D9%88%D9%84%D8%A9/c1413735466";
    private static final String WALL_URL = "https://alnuwaq.com/%D8%AC%D8%AF%D8%A7%D8%B1%D9%8A%D8%A7%D8%AA/c1548018693";
    private static final String ACCESSORIES_URL = "https://alnuwaq.com/%D8%A7%D9%83%D8%B3%D8%B3%D9%88%D8%A7%D8%B1%D8%A7%D8%AA-%D8%A7%D9%84%D8%B7%D8%A7%D9%88%D9%84%D9%87/c2025767156";
    private static final String PARTS_URL = "https://alnuwaq.com/%D8%A7%D8%AC%D8%B2%D8%A7%D8%A1-%D8%B7%D8%A7%D9%88%D9%84%D9%87-%D8%A7%D9%84%D9%8A%D9%83%D8%B3%D8%A7/c1479310264";
    private static final int FILE_CHOOSER_REQUEST_CODE = 9001;

    private WebView webView;
    private ProgressBar progressBar;
    private View splashOverlay;
    private View errorPanel;
    private ValueCallback<Uri[]> filePathCallback;
    private boolean splashDismissed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#130E0A"));
        window.setNavigationBarColor(Color.parseColor("#130E0A"));

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        splashOverlay = findViewById(R.id.splashOverlay);
        errorPanel = findViewById(R.id.errorPanel);

        setupWebView();
        setupNavigation();

        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> {
            errorPanel.setVisibility(View.GONE);
            webView.reload();
        });

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.restoreState(savedInstanceState);
            dismissSplashSoon();
        }
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setUserAgentString(settings.getUserAgentString() + " AlnuwaqAndroid/2.0");

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUri(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                errorPanel.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                dismissSplashSoon();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    errorPanel.setVisibility(View.VISIBLE);
                    dismissSplashSoon();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallbackNew, FileChooserParams fileChooserParams) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = filePathCallbackNew;
                try {
                    Intent chooserIntent = fileChooserParams.createIntent();
                    startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE);
                    return true;
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    return false;
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (ActivityNotFoundException ignored) {
            }
        });
    }

    private void setupNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> load(HOME_URL));
        findViewById(R.id.navCategories).setOnClickListener(v -> showCategoryDialog());
        findViewById(R.id.navTrack).setOnClickListener(v -> load(TRACK_URL));
        findViewById(R.id.navCart).setOnClickListener(v -> load(CART_URL));
        findViewById(R.id.navRefresh).setOnClickListener(v -> webView.reload());

        findViewById(R.id.headerHome).setOnClickListener(v -> load(HOME_URL));
        findViewById(R.id.headerTrack).setOnClickListener(v -> load(TRACK_URL));
    }

    private void showCategoryDialog() {
        final String[] items = {
                "الطاولات",
                "إكسسوارات الطاولة",
                "الجداريات",
                "أجزاء طاولة أليكسا"
        };
        final String[] urls = {TABLES_URL, ACCESSORIES_URL, WALL_URL, PARTS_URL};

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("اختر القسم")
                .setItems(items, (d, which) -> load(urls[which]))
                .setNegativeButton("إغلاق", null)
                .create();
        dialog.setOnShowListener(d -> {
            TextView title = dialog.findViewById(getResources().getIdentifier("alertTitle", "id", "android"));
            if (title != null) {
                title.setTextColor(Color.parseColor("#463120"));
            }
        });
        dialog.show();
    }

    private void load(String url) {
        errorPanel.setVisibility(View.GONE);
        webView.loadUrl(url);
    }

    private boolean handleUri(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();

        if (scheme.equals("http") || scheme.equals("https")) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
        }
        return true;
    }

    private void dismissSplashSoon() {
        if (splashDismissed) return;
        splashDismissed = true;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (splashOverlay.getVisibility() == View.VISIBLE) {
                splashOverlay.animate()
                        .alpha(0f)
                        .setDuration(320)
                        .withEndAction(() -> splashOverlay.setVisibility(View.GONE))
                        .start();
            }
        }, 900);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback == null) return;
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(result);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }
}
