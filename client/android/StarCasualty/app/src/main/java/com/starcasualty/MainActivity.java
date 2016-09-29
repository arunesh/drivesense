package com.starcasualty;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;


public class MainActivity extends Activity {
    private static final int HTML_FILE_SIZE = 12500; /* bytes */

    // Result codes for the download-pdf AsyncTask.
    private static final int PDF_DOWNLOAD_SUCCEEDED = 1;
    private static final int PDF_DOWNLOAD_SDCARD_NOT_WRITABLE = 1001;
    private static final int PDF_DOWNLOAD_INVALID_PARAMS = 1002;
    private static final int PDF_DOWNLOAD_FILE_OPEN_FAILED = 1003;
    private static final int PDF_DOWNLOAD_FAILED = 1004;

    private static final String TAG = "StarCasualty";
    private static final String MIME_TYPE_DEFAULT = "text/html";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final String BASE_PAGE = "http://www.starcasualty.com/app_site";
    private static final String DEFAULT_PDF_FILENAME = "star_casualty.pdf";
    private static final int PDF_READ_SIZE = 1024; /* bytes */
    private static final StrictMode.ThreadPolicy LAX_POLICY = new StrictMode.ThreadPolicy.Builder()
            .permitAll().build();

    private WebView mWebView;
    private String mLocalHtmlFile;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_main);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new MyWebViewClient());
        //loadHtmlToString();
        mWebView.loadUrl(BASE_PAGE);
        //loadWebViewFromLocalHtml(mWebView);
        //mWebView.loadUrl("http://www.starcasualty.com/");
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
        Log.d(TAG, "Starting to load HTML from disk.");
        loadHtmlToString();
        Log.i(TAG, "HTML:" + mLocalHtmlFile);
        webView.loadDataWithBaseURL("http://www.com.starcasualty.com", mLocalHtmlFile,
                MIME_TYPE_DEFAULT, "utf-8", null);
    }

    private void loadWebViewFromLocalHtml1(WebView webView) {
        Log.d(TAG, "Starting to load HTML from disk.");
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
                Log.d(TAG, "Done reading HTML index file.");
            }
        }
        mLocalHtmlFile = htmlBuilder.toString();
        webView.loadDataWithBaseURL("http://www.com.starcasualty.com", mLocalHtmlFile,
                "text/html", DEFAULT_ENCODING, null);
        webView.clearHistory();
    }
    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private static final class DownloadPdfAsyncTask extends AsyncTask<String, Integer, Integer> {
        private ProgressBar mProgressBar;
        private Context mContext;
        private Uri mPdfFileUri;

        DownloadPdfAsyncTask(Context context, ProgressBar progressBar) {
            mContext = context;
            mProgressBar = progressBar;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            mProgressBar.setVisibility(View.GONE);
            switch(result) {
                case PDF_DOWNLOAD_SDCARD_NOT_WRITABLE:
                case PDF_DOWNLOAD_FILE_OPEN_FAILED:
                case PDF_DOWNLOAD_INVALID_PARAMS:
                    Toast.makeText(mContext, "Unable to download PDF.", Toast.LENGTH_LONG);
                    break;
                case PDF_DOWNLOAD_SUCCEEDED:
                    callPdfViewer();
                    break;
            }
        }

        private void callPdfViewer() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(mPdfFileUri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                mContext.startActivity(intent);
            } catch(ActivityNotFoundException e) {
                Log.i(TAG, "No PDF viewer installed.");
                Toast.makeText(mContext, "No PDF viewer installed.", Toast.LENGTH_LONG);
            }
        }

        @Override
        protected Integer doInBackground(String... params) {
            if (params == null || params.length > 1) {
                Log.e(TAG, "Invalid parameters received for async download");
                return PDF_DOWNLOAD_INVALID_PARAMS;
            }
            if (!isExternalStorageWritable()) {
                Log.e(TAG, "External storage is not writable. Unable to download PDF.");
                return PDF_DOWNLOAD_SDCARD_NOT_WRITABLE;
            }
            String urlString = params[0];
            // get an instance of a cookie manager since it has access to our auth cookie
            CookieManager cookieManager = CookieManager.getInstance();
            // get the cookie string for the site.  This looks something like ".ASPXAUTH=data"
            String auth = cookieManager.getCookie(urlString);
            Log.i(TAG, "Auth cookie:" + auth);
            File pdfFile = new File(Environment.getExternalStorageDirectory(), DEFAULT_PDF_FILENAME);
            mPdfFileUri = Uri.fromFile(pdfFile);
            Log.i(TAG, "Writing to: " + pdfFile.getAbsolutePath() + " filename: " + pdfFile.getName());
            FileOutputStream os;
            try {
                os = new FileOutputStream(pdfFile);
            } catch (FileNotFoundException e) {
                Log.i(TAG, "Unable to Open file " + e);
                return PDF_DOWNLOAD_FILE_OPEN_FAILED;
            }
            try {
                URL url = new URL(urlString);
                URLConnection urlConnection = url.openConnection();
                urlConnection.setRequestProperty("Cookie", auth);
                urlConnection.setDoOutput(true);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                byte[] byteArray = new byte[PDF_READ_SIZE];
                boolean writeFinished = false;
                int numRead = 0;
                while (!writeFinished) {
                    numRead = in.read(byteArray);
                    if (numRead > 0 ) {
                        os.write(byteArray, 0, numRead);
                    } else {
                        writeFinished = true;
                    }
                }
                in.close();
                os.close();
                return PDF_DOWNLOAD_SUCCEEDED;
            } catch (IOException e) {
                Log.i(TAG, "Exception attempting to open URL Connection: " + e);
            }
            return PDF_DOWNLOAD_FAILED;
        }
    }

    private class MyWebViewClient extends WebViewClient {

        // This method overrode the onPageFinished().
        private void onPageFinishedOldWithHack(WebView view, String url) {
            Log.v(TAG, "onPageFinished:" + url);
            if (url.endsWith("agentlogin")) {
                Log.v(TAG, "Triggering agentLoginEnable().");
                view.loadUrl("javascript:agentLoginEnable()");
            } else if (url.endsWith("policyholders")) {
                Log.v(TAG, "Triggering policyHolderEnable().");
                view.loadUrl("javascript:policyHoldersEnable()");
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i(TAG, "URL being loaded: " + url);
            // Special case for PPOpenDocument.
            if (url.startsWith("https://www.starcasualty.com/is/policyholderportal/PPOpenDocument.cfm?")) {
//                String newUrl = BASE_PAGE;
//                try {
//                    newUrl = "http://docs.google.com/viewer?embedded=true&url=" + URLEncoder.encode(url, "UTF-8");
//                } catch (UnsupportedEncodingException e) {
//                    Log.e(TAG, "Exception encoding URL: " + url);
//                    return false;
//                }
               // Log.i(TAG, "Instead loading URL:" + newUrl);
               // mWebView.loadUrl(newUrl);

                downloadPdf(url);
                return true;
            }
            if ( (url.startsWith("http:") || url.startsWith("https:")) && !url.endsWith(".pdf")) {
                return false;
            }

            // Otherwise allow the OS to handle it
            // This helps with tel: or mail: links.
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mProgressBar.setVisibility(View.VISIBLE);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mProgressBar.setVisibility(View.GONE);
            super.onPageFinished(view, url);
        }

        private void downloadPdf(String urlString) {
            new DownloadPdfAsyncTask(MainActivity.this, mProgressBar).execute(urlString);
        }

        // TODO: Runtime check of API method (21 versus 15)
 //       @Override
 //       public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
 //           String urlString = request.getUrl().toString();
 //           if (urlString.startsWith("http://www.starcasualty.com/index.cfm")) {
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


        // Code to load web resource from string: Documented here for posterity:
 /*     @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url.startsWith("http://www.com.starcasualty.com/index.cfm")) {
                Log.d(TAG, "NOT going to network for URL:" + url);
                try {
                    return new WebResourceResponse(MIME_TYPE_DEFAULT, DEFAULT_ENCODING,
                            new ByteArrayInputStream(mLocalHtmlFile.getBytes(DEFAULT_ENCODING)));
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "UnsupportedEncodingException: " + e);
                    return null;
                }
            }
            Log.d(TAG, "Going to network for Request: " + url);
            return super.shouldInterceptRequest(view, url);
        }*/

    }
}
