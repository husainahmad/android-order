package com.harmoni.pos.order.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class DailyReportAdapter extends RecyclerView.Adapter<DailyReportAdapter.ReportHolder> {

    private final List<DailyReportViewModel.Row> rows = new ArrayList<>();

    public void submitList(List<DailyReportViewModel.Row> newRows) {
        rows.clear();
        if (newRows != null) {
            rows.addAll(newRows);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_report, parent, false);
        return new ReportHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportHolder holder, int position) {
        DailyReportViewModel.Row row = rows.get(position);
        holder.itemView.setBackgroundResource(position % 2 == 0 ? R.color.row_even : R.color.row_odd);
        holder.date.setText(row.date);
        holder.product.setText(row.productName);
        holder.qty.setText(String.valueOf(row.quantity));
        holder.net.setText(CurrencyUtils.formatRp(row.netSales));
        holder.discount.setText(CurrencyUtils.formatRp(row.discount));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ReportHolder extends RecyclerView.ViewHolder {
        final TextView date;
        final TextView product;
        final TextView qty;
        final TextView net;
        final TextView discount;

        ReportHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.dateText);
            product = itemView.findViewById(R.id.productText);
            qty = itemView.findViewById(R.id.qtyText);
            net = itemView.findViewById(R.id.netText);
            discount = itemView.findViewById(R.id.discountText);
        }
    }
}
