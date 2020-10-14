package com.example.stockwatcher;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class StockViewHolder extends RecyclerView.ViewHolder {

    TextView symbol;
    TextView price;
    TextView priceChanges;
    TextView companyName;

    StockViewHolder(View view) {
        super(view);
        symbol = view.findViewById(R.id.stockSymbolTextView);
        price = view.findViewById(R.id.priceTextView);
        priceChanges = view.findViewById(R.id.priceChangesTextView);
        companyName = view.findViewById(R.id.companyNameTextView);
    }
}
