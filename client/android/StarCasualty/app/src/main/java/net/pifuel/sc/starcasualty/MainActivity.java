package net.pifuel.sc.starcasualty;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;


public class MainActivity extends Activity {
    private static final int HTML_FILE_SIZE = 12500; /* bytes */
    private static final String TAG = "StarCasualty";
    private static final String MIME_TYPE_DEFAULT = "text/html";
    private static final String DEFAULT_ENCODING = "UTF-8";


    private WebView mWebView;
    private String mLocalHtmlFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_main);
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new MyWebViewClient());
        loadHtmlToString();
        mWebView.loadUrl("file:///android_asset/starmain.html");
        //loadWebViewFromLocalHtml(mWebView);
        //mWebView.loadUrl("http://www.starcasualty.com/2015/");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyDown()");
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    private void loadHtmlToString() {
        Log.i(TAG, "Starting to load HTML from disk.");
        StringBuffer stringBuffer = new StringBuffer(HTML_FILE_SIZE);
        InputStream is = getResources().openRawResource(R.raw.starmain);
        if (is == null) {
            Log.e(TAG, "Null inputstream for HTML resource.");
        }
        boolean doneReading = false;
        byte[] buffer = new byte[100];
        while (!doneReading) {
            try {
                int numRead = is.read(buffer);
                if (numRead <= 0) {
                    doneReading = true;
                }
                if (numRead > 0) stringBuffer.append(new String(buffer, 0, numRead, "UTF-8"));
                // Log.i(TAG, "Read line: " + new String(buffer, "UTF-8"));
            } catch (IOException e) {
                doneReading = true;
                Log.i(TAG, "Done reading HTML index file.");
            }
        }
        mLocalHtmlFile = stringBuffer.toString();
    }
    private void loadWebViewFromLocalHtml(WebView webView) {
        Log.i(TAG, "Starting to load HTML from disk.");
        loadHtmlToString();
        Log.i(TAG, "HTML:" + mLocalHtmlFile);
        webView.loadDataWithBaseURL("http://www.starcasualty.com", mLocalHtmlFile,
                MIME_TYPE_DEFAULT, "utf-8", null);
    }

    private void loadWebViewFromLocalHtml1(WebView webView) {
        Log.i(TAG, "Starting to load HTML from disk.");
        StringBuilder htmlBuilder = new StringBuilder(HTML_FILE_SIZE);
        InputStream is = getResources().openRawResource(R.raw.starmain);
        if (is == null) {
            Log.e(TAG, "Null inputstream for HTML resource.");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        boolean doneReading = false;
        while (!doneReading) {
            try {
                String line = reader.readLine();
                htmlBuilder.append(line);
                //Log.i(TAG, "Read line: " + line);
            } catch (IOException e) {
                doneReading = true;
                Log.i(TAG, "Done reading HTML index file.");
            }
        }
        mLocalHtmlFile = htmlBuilder.toString();
        webView.loadDataWithBaseURL("http://www.starcasualty.com", mLocalHtmlFile,
                "text/html", DEFAULT_ENCODING, null);
        webView.clearHistory();
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url.startsWith("http://www.starcasualty.com/2015/index.cfm")) {
                Log.i(TAG, "NOT going to network for URL:" + url);
                try {
                    return new WebResourceResponse(MIME_TYPE_DEFAULT, DEFAULT_ENCODING,
                            new ByteArrayInputStream(mLocalHtmlFile.getBytes(DEFAULT_ENCODING)));
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "UnsupportedEncodingException: " + e);
                    return null;
                }
            }
            Log.i(TAG, "Going to network for Request: " + url);
            return super.shouldInterceptRequest(view, url);
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            Log.i(TAG, "onPageFinished:" + url);
            if (url.endsWith("agentlogin")) {
                Log.i(TAG, "Triggering agentLoginEnable().");
                view.loadUrl("javascript:agentLoginEnable()");
            } else if (url.endsWith("policyholders")) {
                Log.i(TAG, "Triggering policyHolderEnable().");
                view.loadUrl("javascript:policyHoldersEnable()");
            }
        }

        // TODO: Runtime check of API method (21 versus 15)
 //       @Override
 //       public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
 //           String urlString = request.getUrl().toString();
 //           if (urlString.startsWith("http://www.starcasualty.com/2015/index.cfm")) {
  //              try {
  //                  new WebResourceResponse(MIME_TYPE_DEFAULT, DEFAULT_ENCODING,
   //                         new ByteArrayInputStream(mLocalHtmlFile.getBytes(DEFAULT_ENCODING)));
  //              } catch (UnsupportedEncodingException e) {
  ///                  return null;
  //              }
  //          }
  //          Log.i(TAG, "Request: " + request.getUrl());
  //          return super.shouldInterceptRequest(view, request);
 //       }
    }
}
