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
    private android.content.Context context;

    public void submitList(List<DailyReportViewModel.Row> newRows) {
        rows.clear();
        if (newRows != null) {
            rows.addAll(newRows);
        }
        notifyDataSetChanged();
    }

    public void setContext(android.content.Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ReportHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_daily_report, parent, false);
        return new ReportHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportHolder holder, int position) {
        holder.bind(rows.get(position), position);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    // For sticky header decoration — group by date
    public long getHeaderId(int position) {
        if (position < 0 || position >= rows.size()) return -1;
        return rows.get(position).date.hashCode();
    }

    public View getHeaderView(int position) {
        if (context == null || rows.isEmpty() || position < 0 || position >= rows.size()) return null;
        String date = rows.get(position).date;
        View headerView = LayoutInflater.from(context).inflate(R.layout.item_report_header, null, false);
        TextView tv = headerView.findViewById(R.id.headerDateText);
        tv.setText(date);
        // Measure immediately so decoration can use it
        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        headerView.measure(widthSpec, heightSpec);
        headerView.layout(0, 0, headerView.getMeasuredWidth(), headerView.getMeasuredHeight());
        return headerView;
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

        void bind(DailyReportViewModel.Row row, int position) {
            itemView.setBackgroundResource(position % 2 == 0 ? R.color.row_even : R.color.row_odd);
            date.setText(row.date);
            product.setText(row.productName);
            product.setTooltipText(row.productName);
            qty.setText(String.valueOf(row.quantity));
            net.setText(CurrencyUtils.formatRp(row.netSales));
            discount.setText(CurrencyUtils.formatRp(row.discount));
        }
    }
}
