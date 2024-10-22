package com.example.stockwatcher;

import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class StockDownloaderRunnable implements Runnable{

    private static final String TAG = "StockDownloaderRunnable";
    private static String API_KEY = "pk_843875f639654198a420bf7384e1595c";
    private MainActivity mainActivity;
    public Stock stock;
    private String DATA_URL;
    private boolean saveToDb;

    // constructor
    StockDownloaderRunnable(MainActivity mainActivity, Stock stock, boolean saveToDb) {
        this.mainActivity = mainActivity;
        this.stock = stock;
        this.saveToDb = saveToDb;
        this.DATA_URL = "https://cloud.iexapis.com/stable/stock/" +
                        stock.getSymbol() +
                        "/quote?token=" +
                        API_KEY;
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

    // parse raw data, converts to stock object, adds stock to list of sticks in main activity
    private void dataHandler(String s) {
        if (s == null) {
            Log.d(TAG, "dataHandler: Failure in downloading data");
            mainActivity.runOnUiThread(() -> mainActivity.downloadFailed());
            return;
        }
        parseJSON(s);
        // update main activity ui with new stock financial data
        if (saveToDb)
            mainActivity.runOnUiThread(() -> mainActivity.addStockAsSelection(stock));
        else
            mainActivity.runOnUiThread(() -> mainActivity.addStockFromDownloader(stock));
    }

    // parse json string and returns Stock object
    private void parseJSON(String s) {
        //Stock jStock = new Stock();
        try {
            JSONObject jObjMain = new JSONObject(s);
            stock.setSymbol(jObjMain.getString("symbol"));
            stock.setCompany(jObjMain.getString("companyName"));

            // checking stock values that can be null
            String lp = jObjMain.getString("latestPrice");
            if (lp.equals("null"))
                lp = "0";
            stock.setPrice(Double.parseDouble(lp));

            String c = jObjMain.getString("change");
            if (c.equals("null"))
                c = "0";
            stock.setPriceChange(Double.parseDouble(c));

            String cp = jObjMain.getString("changePercent");
            if (cp.equals("null"))
                cp = "0";
            stock.setChangePercentage(Double.parseDouble(cp));

            //return jStock;
        } catch (Exception e) {
            Log.d(TAG, "parseJSON: " + e.getMessage());
            e.printStackTrace();
        }
        //return null;
    }
}
