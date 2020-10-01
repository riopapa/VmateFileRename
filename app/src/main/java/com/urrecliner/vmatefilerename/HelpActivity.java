package com.urrecliner.vmatefilerename;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_help);
            WebView webView = findViewById(R.id.webView);
//            WebSettings settings = webView.getSettings();
//            settings.setJavaScriptEnabled(true);
            webView.loadUrl("file:///android_res/raw/help.html");
        }
}
