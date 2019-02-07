package com.example.conti.mystocks2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.io.Reader;
import java.net.URL;
import java.net.HttpURLConnection;
//import javax.net.ssl.HttpsURLConnection;

/**
 * Implementation of headless Fragment that runs an AsyncTask to fetch data from the network.
 */
public class NetworkFragment extends Fragment
{
    public static final String LOG_TAG = "NetworkFragment";
    private static final String URL_KEY = "UrlKey";
    private DownloadCallback mCallback;
    private DownloadTask mDownloadTask;

    /**
     * Static initializer for NetworkFragment that sets the URL of the host it will be downloading
     * from.
     */
    public static NetworkFragment getInstance(FragmentManager fragmentManager, String url) {
        // Recover NetworkFragment in case we are re-creating the Activity due to a config change.
        // This is necessary because NetworkFragment might have a task that began running before
        // the config change and has not finished yet.
        // The NetworkFragment is recoverable via this method because it calls
        // setRetainInstance(true) upon creation.
        NetworkFragment networkFragment = (NetworkFragment) fragmentManager.findFragmentByTag(NetworkFragment.LOG_TAG);
        if (networkFragment == null) {
            networkFragment = new NetworkFragment();
            Bundle args = new Bundle();
            args.putString(URL_KEY, url);
            networkFragment.setArguments(args);
            fragmentManager.beginTransaction().add(networkFragment, LOG_TAG).commit();
        }
        return networkFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this Fragment across configuration changes in the host Activity.
        setRetainInstance(true);
        //String mUrlString = getArguments().getString(URL_KEY);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Host Activity will handle callbacks from task.
        mCallback = (DownloadCallback)context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Clear reference to host Activity.
        mCallback = null;
    }

    @Override
    public void onDestroy() {
        // Cancel task when Fragment is destroyed.
        cancelDownload();
        super.onDestroy();
    }

    /**
     * Start non-blocking execution of DownloadTask.
     */
    public void startDownload (String stocklist)
    {
        cancelDownload();
        mDownloadTask = new DownloadTask();
        mDownloadTask.execute(stocklist);   // start the download task
    }

    /**
     * Cancel (and interrupt if necessary) any ongoing DownloadTask execution.
     */
    public void cancelDownload() {
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
    }

    /**
     * Implementation of AsyncTask that runs a network operation on a background thread.
     */
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
            if (mCallback != null) {
                NetworkInfo networkInfo = mCallback.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() ||
                    (networkInfo.getType() != ConnectivityManager.TYPE_WIFI && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE))
                {
                    // If no connectivity, cancel task and update Callback with null data.
                    mCallback.updateFromDownload(null);
                    cancel(true);
                }
            }
        }

        /**
         * Defines work to perform on the background thread.
         */
        @Override
        protected String doInBackground(String... s)
        {
            String result = null;
            if (s != null && s.length > 0) {
                try {
                    String symbols = s[0];
                    result = downloadUrl(symbols);
                    if (result == null) {
                        result = "No response received.";
                    }
                } catch(Exception e) {
                    result = e.toString();
                }
            }
            return result;
        }

        /**
         * Send DownloadCallback a progress update.
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (values.length >= 2) {
                mCallback.onProgressUpdate(values[0], values[1]);
            }
        }

        /**
         * Updates the DownloadCallback with the result.
         */
        @Override
        protected void onPostExecute(String result) {
            if (mCallback != null) {
                mCallback.updateFromDownload(result);
                mCallback.finishDownloading();
            }
        }

        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(String result) {
        }

        /**
         * Given a URL, sets up a connection and gets the HTTP response body from the server.
         * If the network request is successful, it returns the response body in String form. Otherwise,
         * it will throw an IOException.
         */
        private String downloadUrl(String symbols) throws IOException
        {
            /*  URL in Yahoo Query Language (YQL)
            https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20csv%20where%20url
            %3D'http%3A%2F%2Fdownload.finance.yahoo.com%2Fd%2Fquotes.csv%3Fs%3D
            BMW.DE%2CDAI.DE%2C%255EGDAXI%26f%3Dsnc4xl1d1t1c1p2ohgv%26e%3D.csv'%20and%20columns%3D'
            symbol%2Cname%2Ccurrency%2Cexchange%2Cprice%2Cdate%2Ctime%2Cchange%2Cpercent%2C
            open%2Chigh%2Clow%2Cvolume'&diagnostics=true";
            */

            // construct the ULR
            final String URL_PARAMETER = "https://query.yahooapis.com/v1/public/yql";
            final String SELECTOR = "select%20*%20from%20csv%20where%20";
            final String DOWNLOAD_URL = "http://download.finance.yahoo.com/d/quotes.csv";
            final String DIAGNOSTICS = "'&diagnostics=false";

            //String symbols = "BMW.DE,DAI.DE";
            //symbols = symbols.replace("^", "%255E");
            String parameters = "snc4xl1d1t1c1p2ohgv";
            String columns = "symbol,name,currency,exchange,price,date,time,change,percent,open,high,low,volume";

            String urlString = URL_PARAMETER;
            urlString += "?q=" + SELECTOR;
            urlString += "url='" + DOWNLOAD_URL;
            urlString += "?s=" + symbols;
            urlString += "%26f=" + parameters;
            urlString += "%26e=.csv'%20and%20columns='" + columns;
            urlString += DIAGNOSTICS;

            Log.v(LOG_TAG, "URL-request: " + urlString);

            InputStream stream = null;
            HttpURLConnection connection = null;

            final boolean boDebug = true; //false;
            if (boDebug == true) {
                String result = sDummyResponse;
                return result;
            } else {

            String result = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                connection.setReadTimeout(3000);
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                connection.setConnectTimeout(3000);
                // For this use case, set HTTP method to GET.
                connection.setRequestMethod("GET");
                // Already true by default but setting just in case; needs to be true since this request
                // is carrying an input (response) body.
                connection.setDoInput(true);
                // Open communications link (network traffic occurs here).
                connection.connect();
                publishProgress(DownloadCallback.Progress.CONNECT_SUCCESS);
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                // Retrieve the response body as an InputStream.
                stream = connection.getInputStream();
                publishProgress(DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0);
                if (stream != null) {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                    String line = "";
                    result = line;
                    while ((line = bufferedReader.readLine()) != null) {
                        result += line + "\n";
                    }
                    // Converts Stream to String with max length of 500.
                    publishProgress(DownloadCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS, 0);
                }
            } finally {
                // Close Stream and disconnect HTTPS connection.
                if (stream != null) {
                    stream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return result;
            }
        }
    }

    private String sDummyResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<query xmlns:yahoo=\"http://www.yahooapis.com/v1/base.rng\" yahoo:count=\"2\" yahoo:created=\"2017-07-25T13:14:30Z\" yahoo:lang=\"en-US\">" +
        "<results><row><symbol>BMW.DE</symbol><name>BAY.MOTOREN WERKE AG ST</name><currency>EUR</currency><exchange>GER</exchange><price>79.03</price><date>7/25/2017</date>" +
        "<time>9:56am</time><change>+0.09</change><percent>+0.11%</percent><open>79.07</open><high>79.20</high><low>78.60</low><volume>321977</volume></row>" +
        "<row><symbol>DAI.DE</symbol><name>DAIMLER AG NA O.N.</name><currency>EUR</currency><exchange>GER</exchange><price>61.39</price><date>7/25/2017</date><time>9:57am</time>" +
        "<change>+0.47</change><percent>+0.77%</percent><open>61.23</open><high>61.50</high><low>61.00</low><volume>1048248</volume></row></results></query><!-- total: 1 -->";

}
