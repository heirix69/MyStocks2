package com.example.conti.mystocks2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.support.v4.widget.SwipeRefreshLayout;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



public class MainActivity extends AppCompatActivity implements DownloadCallback

{
    public final String LOG_TAG = "MyStocks2";

    // adapter showing a StringArray in the ListView
    private ArrayAdapter <String> mArrayAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout1;

    // Reference to the TextView showing fetched data, so we can clear it with a button as necessary.
    private TextView mReceivedData;

    // Keep a reference to the NetworkFragment which owns the AsyncTask object
    private NetworkFragment mNetworkFragment;

    // flag indicating if a download is in progress
    private boolean mDownloading = false;

    // string containing the symbols of the stocklist
    private String mSymbols;

    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String prefKey = getString(R.string.pref_stocklist_key);
        String prefDefault = getString(R.string.pref_stocklist_default);
        mSymbols = sharedPrefs.getString(prefKey, prefDefault);

        sharedPrefs.registerOnSharedPreferenceChangeListener(onPrefChange);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View view)
            {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mSwipeRefreshLayout1 = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout1);
        mSwipeRefreshLayout1.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });

        String[] mStocklist = mSymbols.split("[,;\\s]");
        List <String> list = new ArrayList<>(Arrays.asList(mStocklist)); //saDummyList));
        mArrayAdapter = new ArrayAdapter<>( getApplicationContext(),
                                             R.layout.list_item_textview, // ID xml-layout file
                                             R.id.tvListItem1, // ID of TextView
                                             list );

        ListView listViewStocks = (ListView)findViewById(R.id.listView1);
        listViewStocks.setAdapter(mArrayAdapter);
        mReceivedData = new TextView(getApplicationContext());
        mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), "http://finance.yahoo.com");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(onPrefChange);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.menu_refresh) {
            refreshData();
            return true;
        }
        if (id == R.id.menu_settings) {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SettingsActivity.class);
            //String message = "hallo";
            //intent.putExtra(EXTRA_MESSAGE, message);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshData()
    {
        Toast.makeText(getApplicationContext(), "Refresh", Toast.LENGTH_SHORT).show();
        if (!mDownloading && mNetworkFragment != null) {
            mNetworkFragment.startDownload(mSymbols);
            mDownloading = true;
        }
    }

    private String[] extractDataFromXML (String xmlString)
    {
        Document doc;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xmlString));
            doc = db.parse(is);
        } catch (ParserConfigurationException e) {
            Log.e(LOG_TAG,"Parser Error: " + e.getMessage());
            return null;
        } catch (SAXException e) {
            Log.e(LOG_TAG,"SAX Error: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(LOG_TAG,"IO Error: " + e.getMessage());
            return null;
        }

        Element xmlData = doc.getDocumentElement();
        NodeList stockList = xmlData.getElementsByTagName("row");

        int iNumOfStocks = stockList.getLength();
        int iNumOfParams = stockList.item(0).getChildNodes().getLength();

        String[] saOutput = new String[iNumOfStocks];
        String[][] saAllData = new String[iNumOfStocks][iNumOfParams];

        Node nodeParam;
        String nodeParamValue;
        for( int i=0; i<iNumOfStocks; i++ ) {
            NodeList nodelist = stockList.item(i).getChildNodes();

            for (int j=0; j<iNumOfParams; j++) {
                nodeParam = nodelist.item(j);
                nodeParamValue = nodeParam.getFirstChild().getNodeValue();
                saAllData[i][j] = nodeParamValue;
            }

            saOutput[i]  = saAllData[i][0];                // symbol
            saOutput[i] += ": " + saAllData[i][4];         // price
            saOutput[i] += " " + saAllData[i][2];          // currency
            saOutput[i] += " (" + saAllData[i][8] + ")";   // percent
            saOutput[i] += " - [" + saAllData[i][1] + "]"; // name

            Log.v(LOG_TAG,"XML Output:" + saOutput[i]);
        }
        return saOutput;
    }


    @Override
    public void updateFromDownload(String result) {
        if (result != null) {
            Log.d(LOG_TAG, result);
            mReceivedData.setText(result);
            String[] saData = extractDataFromXML(result);
            if (saData != null) {
                mArrayAdapter.clear();
                for (String s : saData) {
                    mArrayAdapter.add(s);
                }
            }
        } else {
            Log.e(LOG_TAG,"Download error.");
            mReceivedData.setText(getString(R.string.connection_error));
            Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    @Override
    public void finishDownloading() {
        mDownloading = false;
        if (mNetworkFragment != null) {
            mNetworkFragment.cancelDownload();
        }
        mSwipeRefreshLayout1.setRefreshing(false);
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        switch(progressCode) {
            // You can add UI behavior for progress updates here.
            case Progress.ERROR:
                break;
            case Progress.CONNECT_SUCCESS:
                break;
            case Progress.GET_INPUT_STREAM_SUCCESS:
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                mReceivedData.setText("" + percentComplete + "%");
                break;
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                break;
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener onPrefChange =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                String prefKey = getString(R.string.pref_stocklist_key);
                String prefDefault = getString(R.string.pref_stocklist_default);
                if (key.equals(prefKey)) {
                    mSymbols = prefs.getString(prefKey, prefDefault);
                    Log.v(LOG_TAG,"onPrefChange " + key + ": " + prefDefault);
                }
            }
        };

//    String[] saDummyList = {
//                    "Adidas - Kurs: 73,45 €",
//                    "Allianz - Kurs: 145,12 €",
//                    "BASF - Kurs: 84,27 €",
//                    "Bayer - Kurs: 128,60 €",
//                    "Beiersdorf - Kurs: 80,55 €",
//                    "BMW St. - Kurs: 104,11 €",
//                    "Commerzbank - Kurs: 12,47 €",
//                    "Continental - Kurs: 209,94 €",
//                    "Daimler - Kurs: 84,33 €"
//            };

//    public class DataHolder
//    {
//        private String mystring;
//        public String getData() {return mystring;}
//        public void setData(String s) {this.mystring = s;}
//
//        private static final DataHolder holder = new DataHolder();
//        public static DataHolder getInstance() {return holder;}
//    }


//    SharedPreferences myprefs= this.getSharedPreferences("user", MODE_WORLD_READABLE);
//    myprefs.edit().putString("session_id", value).commit();
//    SharedPreferences myprefs= getSharedPreferences("user", MODE_WORLD_READABLE);
//    String session_id= myprefs.getString("session_id", null);
}
