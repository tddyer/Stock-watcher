package com.example.stockwatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener{

    // stocks selected to be displayed in recycler view
    private List<Stock> stocksList = new ArrayList<>();

    // stock symbols + names to be used for downloading stock data
    private HashMap<String, String> stockNames = new HashMap<>();

    private RecyclerView recyclerView;
    private StocksAdapter stocksAdapter;
    private DatabaseHandler databaseHandler;
    private SwipeRefreshLayout swiper;

    private static final String marketWatchURL = "http://www.marketwatch.com/investing/stock/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.stocksRecyclerView);
        stocksAdapter = new StocksAdapter(stocksList, this);
        recyclerView.setAdapter(stocksAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        databaseHandler = new DatabaseHandler(this);


        // swipe refresher
        swiper = findViewById(R.id.swiper);
        swiper.setOnRefreshListener(() -> refreshStocks());



        // fetching stock name data
        NameDownloaderRunnable nameDownloaderRunnable =
                new NameDownloaderRunnable(this);
        new Thread(nameDownloaderRunnable).start();

        // load stock names from internal DB
        ArrayList<Stock> selectedStocks = databaseHandler.loadStocks();

        // checking network connection
        if (networkCheck()) {
            for (Stock stock : selectedStocks) {
                // fetch stock data from IEX
                StockDownloaderRunnable stockDownloaderRunnable =
                        new StockDownloaderRunnable(this, stock, false);
                new Thread(stockDownloaderRunnable).start();

            }
        } else {
            connectionError();
            for (Stock stock : selectedStocks)
                addStockWithoutConnection(stock);
        }
    }

    @Override
    protected void onDestroy() {
        databaseHandler.shutDown();
        super.onDestroy();
    }

    // overriding onClickListener methods


    @Override
    public void onClick(View v) {
        int pos = recyclerView.getChildLayoutPosition(v);
        Stock s = stocksList.get(pos);

        // open MarketWatch stock page
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(marketWatchURL + s.getSymbol()));
        startActivity(i);
    }

    @Override
    public boolean onLongClick(View v) {
        int pos = recyclerView.getChildLayoutPosition(v);
        deleteStock(pos);
        return true;
    }

    // saves stock symbol:name hashmap in app for later usage in downloading stock data
    public void udpateStockNamesMap(HashMap<String, String> stocks) {
        for (Map.Entry<String, String> entry : stocks.entrySet()) {
            stockNames.put(entry.getKey(), entry.getValue());
        }
    }

    public void downloadFailed() {
        Log.d("MainActivity", "downloadFailed: Couldn't download stock names");
    }

    // adds stock to stockList that is already stored in local database
    public void addStockFromDownloader(Stock s) {

        // checking if stock is already displayed
        for(Stock stock : stocksList) {
            if (stock.getSymbol().equals(s.getSymbol())) {
                duplicateStock(s);
                return;
            }
        }

        stocksList.add(s);
        stocksList.sort(new StockSorter());
        stocksAdapter.notifyDataSetChanged();
    }

    // adds stock to stockList that is not already stored in local database
    public void addStockAsSelection(Stock s) {
        // checking if stock is already displayed
        for(Stock stock : stocksList) {
            if (stock.getSymbol().equals(s.getSymbol())) {
                duplicateStock(s);
                return;
            }
        }

        stocksList.add(s);
        stocksList.sort(new StockSorter());
        stocksAdapter.notifyDataSetChanged();
        databaseHandler.addStock(s);
    }

    // without connect just add stocks to list with values defaults to zero
    public void addStockWithoutConnection(Stock s) {
        System.out.println(stocksList);
        // defaulting stock values to zero
        s.setPrice(0.00);
        s.setPriceChange(0.00);
        s.setChangePercentage(0.00);

        stocksList.add(s);
        stocksList.sort(new StockSorter());
        stocksAdapter.notifyDataSetChanged();

    }

    // swipe refresh

    private void refreshStocks() {
        if (networkCheck()) {
            // flush out current stock data
            stocksList.clear();
            stocksAdapter.notifyDataSetChanged();

            // load stock names from internal DB
            ArrayList<Stock> selectedStocks = databaseHandler.loadStocks();

            // refresh by fetching new stocks data
            for (Stock stock : selectedStocks) {
                // fetch stock data from IEX
                StockDownloaderRunnable stockDownloaderRunnable =
                        new StockDownloaderRunnable(this, stock, false);
                new Thread(stockDownloaderRunnable).start();
            }
        } else {
            connectionError();
        }
        swiper.setRefreshing(false);
    }

    // network checking

    private boolean networkCheck() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            Log.d("MainActivity", "networkCheck: Error accessing connectivity manager");

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            // checking if stockNames is empty to repopulate if needed
            //      - occurs if the app was loaded w/o network connection
            //        and gained connection during usage
            if (stockNames.size() == 0) {
                NameDownloaderRunnable nameDownloaderRunnable =
                        new NameDownloaderRunnable(this);
                new Thread(nameDownloaderRunnable).start();
            }
            return true;
        }

        return false;
    }


    // add stock menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_stock_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuAddStock) {
            if (networkCheck()) {
                addStock();
                return true;
            } else {
                connectionError();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    // alert dialogs

    public void addStock() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setGravity(Gravity.CENTER);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, id) -> {
            // add stock
            String inputSymbol = String.valueOf(input.getText());
            HashMap<String, String> matches = findStockNames(inputSymbol);
            if (matches.size() > 1)
                listStockMatches(matches);
            else if (matches.size() == 1) {

                Stock temp = new Stock();

                for (Map.Entry<String, String> entry : matches.entrySet()) {
                    temp.setSymbol(entry.getKey());
                    temp.setCompany(entry.getValue());
                }

                // add stock to recycler view list
                StockDownloaderRunnable stockDownloaderRunnable =
                        new StockDownloaderRunnable(this, temp, true);
                new Thread(stockDownloaderRunnable).start();
            } else {
                noMatchingStocksFound(inputSymbol);
            }
        });
        builder.setNegativeButton("CANCEL", (dialog, id) -> {
            // do nothing
        });
        builder.setTitle("Stock Selection");
        builder.setMessage("Please enter a stock symbol");

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // displays stocks that match the input of the user (if more than one match is found)
    public void listStockMatches(HashMap<String, String> matches) {

        ArrayList<String> matchesArray = new ArrayList<>();
        for (Map.Entry<String, String> entry : matches.entrySet())
            matchesArray.add(entry.getKey() + " - " + entry.getValue());


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Make a selection");


        CharSequence[] matchesList = matchesArray.toArray(new String[matchesArray.size()]);
        builder.setItems(matchesList, (dialog, which) -> {

            String symbol = matchesList[which].toString();
            symbol = symbol.substring(0, symbol.indexOf(" "));

            Stock temp = new Stock();
            temp.setSymbol(symbol);
            temp.setCompany(matches.get(symbol));

            // add stock to recycler view list
            StockDownloaderRunnable stockDownloaderRunnable =
                    new StockDownloaderRunnable(this, temp, true);
            new Thread(stockDownloaderRunnable).start();

        });

        builder.setNegativeButton("Nevermind", (dialog, id) -> {
            // do nothing
        });
        AlertDialog dialog = builder.create();

        dialog.show();
    }

    public void duplicateStock(Stock s) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Stock Symbol " + s.getSymbol() + " is already displayed");
        builder.setTitle("Duplicate Stock");

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void noMatchingStocksFound(String symbol) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Data for stock symbol");
        builder.setTitle("Symbol Not Found: " + symbol);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void connectionError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Stocks cannot be updated without a network connection");
        builder.setTitle("No Network Connection");

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void deleteStock(final int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("OK", (dialog, id) -> removePos(pos));
        builder.setNegativeButton("CANCEL", (dialog, id) -> {
            // do nothing
        });
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setTitle("Delete Note");

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // helpers

    public HashMap<String, String> findStockNames(String input) {

        HashMap<String, String> matches = new HashMap<>();

        // check all stock symbols + names for user input to find matches
        for (Map.Entry<String, String> entry : stockNames.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            String v_upper = v.toUpperCase();
            if (k.contains(input) || v_upper.contains(input)) {
                matches.put(k, v);
            }
        }

        return matches;
    }

    public void removePos(int pos) {
        if (!stocksList.isEmpty()) {
            databaseHandler.deleteStock(stocksList.get(pos).getSymbol());
            stocksList.remove(pos);
            stocksAdapter.notifyDataSetChanged();
        }
    }
}