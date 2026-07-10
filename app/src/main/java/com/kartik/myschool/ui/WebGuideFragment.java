package com.kartik.myschool.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kartik.myschool.R;

public class WebGuideFragment extends Fragment {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web_guide, container, false);

        webView = view.findViewById(R.id.webView);
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient());
        
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        String url = "https://edu-report.in/how-to-use";
        if (getArguments() != null && getArguments().containsKey("url")) {
            url = getArguments().getString("url");
        }

        if (url.endsWith(".mp4")) {
            String html = "<html><body style='margin:0;padding:0;background:black;display:flex;justify-content:center;align-items:center;'><video controls autoplay width='100%' style='max-height:100vh;'><source src='" + url + "' type='video/mp4'></video></body></html>";
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        } else {
            webView.loadUrl(url);
        }

        return view;
    }

    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void navigateHome() {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    androidx.navigation.NavController navController = androidx.navigation.fragment.NavHostFragment.findNavController(WebGuideFragment.this);
                    navController.navigateUp();
                });
            }
        }
    }

    // Allow WebView to handle internal back navigation if needed,
    // though the user will use the physical back button or toolbar back arrow.
    public boolean handleBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }
}
