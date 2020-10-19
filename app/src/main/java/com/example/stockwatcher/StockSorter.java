package com.example.stockwatcher;

import java.util.Comparator;

public class StockSorter implements Comparator<Stock> {

    @Override
    public int compare(Stock o1, Stock o2) {
        return o1.getSymbol().compareToIgnoreCase(o2.getSymbol());
    }
}
