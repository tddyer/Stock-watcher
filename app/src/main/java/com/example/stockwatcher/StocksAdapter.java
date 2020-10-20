package com.example.stockwatcher;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

public class StocksAdapter extends RecyclerView.Adapter<StockViewHolder> {

    private List<Stock> stocksList;
    private MainActivity mainActivity;

    private static DecimalFormat df = new DecimalFormat("0.00");

    public StocksAdapter(List<Stock> list, MainActivity ma) {
        this.stocksList = list;
        mainActivity = ma;
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stock_list_card, parent, false);

        itemView.setOnClickListener(mainActivity);
        itemView.setOnLongClickListener(mainActivity);

        return new StockViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        Stock stock = stocksList.get(position);
        String stockDirection = "▲ ";
        int fontColor = Color.parseColor("#00FF00");
        if (stock.getPriceChange() < 0) {
            fontColor = Color.parseColor("#FF0000");
            stockDirection = "▼ ";
        }

        holder.symbol.setText(stock.getSymbol());
        holder.symbol.setTextColor(fontColor);
        holder.price.setText(String.valueOf(df.format(stock.getPrice())));
        holder.price.setTextColor(fontColor);
        String priceChangesString = stockDirection + String.valueOf(df.format(stock.getPriceChange()))
                + " (" + String.valueOf(df.format(stock.getChangePercentage())) + "%)";
        holder.priceChanges.setText(priceChangesString);
        holder.priceChanges.setTextColor(fontColor);
        holder.companyName.setText(stock.getCompany());
        holder.companyName.setTextColor(fontColor);
    }

    @Override
    public int getItemCount() {
        return stocksList.size();
    }
}
