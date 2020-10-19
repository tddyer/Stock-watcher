package com.example.stockwatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener{

    private List<Stock> stocksList = new ArrayList<>();
    private RecyclerView recyclerView;
    private StocksAdapter stocksAdapter;

    private DatabaseHandler databaseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        for (int i = 0; i < 30; i++) {
//            Stock s = new Stock();
//            s.setSymbol("AAAA");
//            s.setPrice(i * 2.22);
//            if (i % 2 == 0) {
//                s.setPriceChange(i + .34);
//                s.setChangePercentage(i + .02);
//            } else {
//                s.setPriceChange((i + .34) * -1);
//                s.setChangePercentage((i + .02) * -1);
//            }
//
//            s.setCompany("Random Company Here");
//            stocksList.add(s);
//        }

        recyclerView = findViewById(R.id.stocksRecyclerView);

        stocksAdapter = new StocksAdapter(stocksList, this);
        recyclerView.setAdapter(stocksAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        databaseHandler = new DatabaseHandler(this);

        // fetch DB file from file system. If it doesn't exist, run nameDownloader runnable
        // to create it
        File dbFile = this.getDatabasePath(DatabaseHandler.DATABASE_NAME);
        if (!(dbFile.exists())) {
            // fetching stock name data
            NameDownloaderRunnable nameDownloaderRunnable =
                    new NameDownloaderRunnable(this);
            new Thread(nameDownloaderRunnable).start();
        }

        // load stock names from internal DB
        ArrayList<Stock> stockNames = databaseHandler.loadStocks();

//        for (Stock stock : stockNames) {
        for(int i = 0; i < 3; i++) {
            // fetch stock data from IEX
            StockDownloaderRunnable stockDownloaderRunnable =
                    new StockDownloaderRunnable(this, stockNames.get(i));
            new Thread(stockDownloaderRunnable).start();

        }

        // test: populates recycler with names from db

//        stocksList.clear();
//        stocksList.addAll(tempStocks);
//        stocksAdapter.notifyDataSetChanged();

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

    public void saveStockNames(HashMap<String, String> stocks) {
        for (String key : stocks.keySet()) {
            Stock temp = new Stock();
            temp.setSymbol(key);
            temp.setCompany(stocks.get(key));
            databaseHandler.addStock(temp);
        }
        stocksAdapter.notifyDataSetChanged();
    }

    public void downloadFailed() {
        stocksList.clear();
        stocksAdapter.notifyDataSetChanged();
    }

    // StockDownloader methods

    // add stock to stocks list, sort list, update changes
    public void addStockFromDownloader(Stock s) {
        stocksList.add(s);
        stocksList.sort(new StockSorter());
        stocksAdapter.notifyDataSetChanged();
    }
}