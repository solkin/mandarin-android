package com.tomclaw.mandarin.im.vk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.main.ChiefActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 8/9/13
 * Time: 1:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class VkAccountAddActivity extends ChiefActivity {

    private AccountRoot accountRoot;

    private WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain class name extra to setup AccountRoot type.
        String className = getIntent().getStringExtra(CLASS_NAME_EXTRA);
        Log.d(Settings.LOG_TAG, "VkAccountAddActivity start for " + className);
        try {
            Class<? extends AccountRoot> accountRootClass = Class.forName(className).asSubclass(AccountRoot.class);
            accountRoot = accountRootClass.newInstance();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onCoreServiceReady() {
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.accounts);
        // Initialize accounts list
        setContentView(accountRoot.getAccountLayout());

        webView = (WebView) findViewById(R.id.vk_web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.clearCache(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(Settings.LOG_TAG, "load url = " + url);
                parseUrl(url);
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        webView.loadUrl(VKSession.getAuthUrl());
        //webView.setVisibility(View.VISIBLE);
    }

    private void parseUrl(String url) {
        try {
            if( url == null ) {
                return;
            }
            Log.d(Settings.LOG_TAG, "redirect url = " + url);
            if( url.startsWith(VKSession.redirect_url) ) {
                if( !url.contains("error") ) {
                    String uid = extractPattern(url, "user_id=(\\d*)");
                    String token = extractPattern(url, "access_token=(.*?)&");
                    String expire_in =  extractPattern(url, "expires_in=(.*?)&");
                    if (uid == null || uid.isEmpty() || token == null || token.isEmpty()){
                        setResult(RESULT_CANCELED);
                    }
                    else{
                        accountRoot.setUserId(uid);
                        accountRoot.setUserNick(uid);
                        ((VkAccountRoot) accountRoot).setToken(token);
                        ((VkAccountRoot) accountRoot).setTokenExpirationDate(System.currentTimeMillis() / 1000L + Long.parseLong(expire_in));
                        getServiceInteraction().addAccount(accountRoot);
                        setResult(RESULT_OK);
                    }
                } else {
                    setResult(RESULT_CANCELED);
                }
            } else if( url.contains("error?err") ) {
                setResult(RESULT_CANCELED);
            }
        } catch( Exception e ) {
            e.printStackTrace();
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    public static String extractPattern(String string, String pattern){
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(string);
        if (!m.find())
            return null;
        return m.toMatchResult().group(1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onCoreServiceDown() {
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }
}
