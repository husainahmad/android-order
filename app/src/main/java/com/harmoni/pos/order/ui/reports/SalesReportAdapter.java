package com.harmoni.pos.order.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.SalesReportRow;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class SalesReportAdapter extends RecyclerView.Adapter<SalesReportAdapter.ReportHolder> {

    private final List<SalesReportRow> rows = new ArrayList<>();

    public void submitList(List<SalesReportRow> newRows) {
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
                .inflate(R.layout.item_sales_report, parent, false);
        return new ReportHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportHolder holder, int position) {
        SalesReportRow row = rows.get(position);
        holder.itemView.setBackgroundResource(position % 2 == 0 ? R.color.row_even : R.color.row_odd);
        holder.category.setText(row.getCategoryName());
        holder.product.setText(row.getProductName());
        holder.qty.setText(String.valueOf(row.getQuantity()));
        holder.gross.setText(CurrencyUtils.formatRp(row.getGrossSales()));
        holder.discount.setText(CurrencyUtils.formatRp(row.getDiscount()));
        holder.net.setText(CurrencyUtils.formatRp(row.getNetSales()));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ReportHolder extends RecyclerView.ViewHolder {
        final TextView category;
        final TextView product;
        final TextView qty;
        final TextView gross;
        final TextView discount;
        final TextView net;

        ReportHolder(@NonNull View itemView) {
            super(itemView);
            category = itemView.findViewById(R.id.categoryText);
            product = itemView.findViewById(R.id.productText);
            qty = itemView.findViewById(R.id.qtyText);
            gross = itemView.findViewById(R.id.grossText);
            discount = itemView.findViewById(R.id.discountText);
            net = itemView.findViewById(R.id.netText);
        }
    }
}
