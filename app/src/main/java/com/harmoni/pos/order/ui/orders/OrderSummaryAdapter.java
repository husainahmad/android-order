package com.harmoni.pos.order.ui.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderSummaryAdapter extends RecyclerView.Adapter<OrderSummaryAdapter.SummaryHolder> {

    private final List<Map.Entry<String, Integer>> items = new ArrayList<>();

    public void submitList(List<Map.Entry<String, Integer>> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SummaryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_summary, parent, false);
        return new SummaryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryHolder holder, int position) {
        Map.Entry<String, Integer> entry = items.get(position);
        holder.productName.setText(entry.getKey());
        holder.quantity.setText(String.valueOf(entry.getValue()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SummaryHolder extends RecyclerView.ViewHolder {
        final TextView productName;
        final TextView quantity;

        SummaryHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productNameText);
            quantity = itemView.findViewById(R.id.quantityText);
        }
    }
}
