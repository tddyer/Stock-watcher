package com.example.stockwatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.stocksRecyclerView);
        stocksAdapter = new StocksAdapter(stocksList, this);
        recyclerView.setAdapter(stocksAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        databaseHandler = new DatabaseHandler(this);


        // fetching stock name data
        NameDownloaderRunnable nameDownloaderRunnable =
                new NameDownloaderRunnable(this);
        new Thread(nameDownloaderRunnable).start();

        // load stock names from internal DB
        ArrayList<Stock> selectedStocks = databaseHandler.loadStocks();

        for (Stock stock : selectedStocks) {
            // fetch stock data from IEX
            StockDownloaderRunnable stockDownloaderRunnable =
                    new StockDownloaderRunnable(this, stock, false);
            new Thread(stockDownloaderRunnable).start();

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
        // TODO: open market watch here
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
        stocksList.clear();
        stocksAdapter.notifyDataSetChanged();
    }

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


    // add stock menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_stock_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuAddStock) {
            addStock();
            return true;
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
            HashMap<String, String> matches = findStockNames(String.valueOf(input.getText()));
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