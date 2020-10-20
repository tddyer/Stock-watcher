package com.example.stockwatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.stocksRecyclerView);

        stocksAdapter = new StocksAdapter(stocksList, this);
        recyclerView.setAdapter(stocksAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        databaseHandler = new DatabaseHandler(this);

        // deletes db
//        this.deleteDatabase(DatabaseHandler.DATABASE_NAME);

        // fetching stock name data
        NameDownloaderRunnable nameDownloaderRunnable =
                new NameDownloaderRunnable(this);
        new Thread(nameDownloaderRunnable).start();

        // load stock names from internal DB
        ArrayList<Stock> selectedStocks = databaseHandler.loadStocks();


        // TODO: Might need to switch this to index so i can edit the stock values from runnable
        for (Stock stock : selectedStocks) {
            // fetch stock data from IEX
            StockDownloaderRunnable stockDownloaderRunnable =
                    new StockDownloaderRunnable(this, stock);
            new Thread(stockDownloaderRunnable).start();

            // after returning from stockDownloader, add stock (now complete with
            // stock financial data) to stockList, sort, and notify adapter of change
            stocksList.add(stock);
            stocksList.sort(new StockSorter());
            stocksAdapter.notifyDataSetChanged();
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
    }

    @Override
    public boolean onLongClick(View v) {
        int pos = recyclerView.getChildLayoutPosition(v);
        // TODO: DELETE STOCK HERE
        return true;
    }

    // saves stock symbol:name hashmap in app for later usage in downloading stock data
    public void udpateStockNamesMap(HashMap<String, String> stocks) {
        for (Map.Entry<String, String> entry : stocks.entrySet()) {
            stockNames.put(entry.getKey(), entry.getValue());
        }
    }

    public void downloadFailed() {
        stocksList.clear();
        stocksAdapter.notifyDataSetChanged();
    }

    // StockDownloader methods

    // add stock to stocks list, sort list, update changes
//    public void addStockFromDownloader(Stock s) {
//        stocksList.add(s);
////        stocksList.sort(new StockSorter());
////        stocksAdapter.notifyDataSetChanged();
//    }

    // add stock menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_stock_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuAddStock) {
            for (Map.Entry<String, String> entry : stockNames.entrySet()) {
                System.out.println(entry.getKey() + ", " + entry.getValue());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}