package com.example.stockwatcher;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;

public class NameDownloaderRunnable implements Runnable{

    private static final String TAG = "NameDownloaderRunnable";
    private MainActivity mainActivity;
    private static final String DATA_URL = "https://api.iextrading.com/1.0/ref-data/symbols";
    public HashMap<String, String> namesMap = new HashMap<>();

    NameDownloaderRunnable(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }


    @Override
    public void run() {
        Uri dataUri = Uri.parse(DATA_URL);
        String usageURL = dataUri.toString();

        StringBuilder sb = new StringBuilder();
        try {
            // connecting to URL
            URL url = new URL(usageURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            // ensuring connection is OK
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "run: HTTP ResponseCode NOT OK - not connected to data source" +
                        conn.getResponseCode());
                dataHandler(null);
                return;
            }

            // appending fetched data to string builder for later handling
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader((new InputStreamReader(is)));

            String line;
            while((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }

        } catch (Exception e) {
            Log.d(TAG, "run: exception", e);
            dataHandler(null);
            return;
        }

        dataHandler(sb.toString());

    }

    private void dataHandler(String s) {
        if (s == null) {
            Log.d(TAG, "dataHandler: Failure in downloading data");
            mainActivity.runOnUiThread(() -> mainActivity.downloadFailed());
            return;
        }

        parseJSON(s);
        mainActivity.runOnUiThread(() -> mainActivity.udpateStockNamesMap(namesMap));
    }

    private void parseJSON(String s) {
        try {
            JSONArray jObjMain = new JSONArray(s);
            for (int i = 0; i < jObjMain.length(); i++) {
                JSONObject jStock = (JSONObject) jObjMain.get(i);
                String symbol = jStock.getString("symbol");
                String company = jStock.getString("name");
                namesMap.put(symbol, company);
            }
        } catch (Exception e) {
            Log.d(TAG, "parseJSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
