package com.example.stockwatcher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Currency;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHandler";
    private static final int DATABASE_VERSION = 1; // increment when DB Schema is changed

    public static final String DATABASE_NAME = "StockAppDB";
    private static final String TABLE_NAME = "StockWatchTable";

    // DB columns
    private static final String SYMBOL = "StockSymbol";
    private static final String COMPANY = "CompanyName";

    // DB creation string
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    SYMBOL + " TEXT not null unique," +
                    COMPANY + " TEXT not null)";

    private SQLiteDatabase database;

    DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        database = getWritableDatabase(); // creates DB from SQLiteOpenHelper
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE); // if db doesn't exist, create it
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    // add stock to db
    public void addStock(Stock stock) {

        ContentValues vals = new ContentValues();
        vals.put(SYMBOL, stock.getSymbol());
        vals.put(COMPANY, stock.getCompany());

        database.insert(TABLE_NAME, null, vals);
    }

    // delete stock from db
    public void deleteStock(String symbol) {
        Log.d(TAG, "deleteStock: Deleting stock " + symbol);

        int count = database.delete(TABLE_NAME, "StockSymbol = ?", new String[] {symbol});

        Log.d(TAG, "deleteStock: " + count);
    }

    // fetch all stocks from db
    public ArrayList<Stock> loadStocks() {
        ArrayList<Stock> stocks = new ArrayList<>();

        Cursor cursor = database.query(
                TABLE_NAME,
                new String[]{SYMBOL, COMPANY},
                null,
                null,
                null,
                null,
                null
        );

        if (cursor != null) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String symbol = cursor.getString(0);
                String company = cursor.getString(1);
                Stock temp = new Stock();
                temp.setSymbol(symbol);
                temp.setCompany(company);
                stocks.add(temp);
                cursor.moveToNext();
            }
            cursor.close();
        }
        Log.d(TAG, "loadStocks: DONE");
        return stocks;
    }

    void dumpDbToLog() {
        Cursor cursor = database.rawQuery("select * from " + TABLE_NAME, null);
        if (cursor != null) {
            cursor.moveToFirst();

            Log.d(TAG, "dumpDbToLog: vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
            for (int i = 0; i < cursor.getCount(); i++) {
                String symbol = cursor.getString(0);
                String company = cursor.getString(1);
                Log.d(TAG, "dumpDbToLog: " +
                        String.format("%s %-18s", SYMBOL + ":", company) +
                        String.format("%s %-18s", COMPANY + ":", symbol));
                cursor.moveToNext();
            }
            cursor.close();
        }

        Log.d(TAG, "dumpDbToLog: ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    }

    void shutDown() {
        database.close();
    }
}
