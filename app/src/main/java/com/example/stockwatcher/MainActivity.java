package com.example.stockwatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener{

    private List<Stock> stocksList = new ArrayList<>();
    private RecyclerView recyclerView;
    private StocksAdapter stocksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < 30; i++) {
            Stock s = new Stock();
            s.setSymbol("AAAA");
            s.setPrice(i * 2.22);
            if (i % 2 == 0) {
                s.setPriceChange(i + .34);
                s.setChangePercentage(i + .02);
            } else {
                s.setPriceChange((i + .34) * -1);
                s.setChangePercentage((i + .02) * -1);
            }

            s.setCompany("Random Company Here");
            stocksList.add(s);
        }

        recyclerView = findViewById(R.id.stocksRecyclerView);

        stocksAdapter = new StocksAdapter(stocksList, this);
        recyclerView.setAdapter(stocksAdapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));


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
        // TODO: DELETE NOTE HERE
        return true;
    }
}