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

public class DailyReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROW = 1;

    private final List<Object> items = new ArrayList<>();
    private android.content.Context context;

    public void submitList(List<DailyReportViewModel.Row> newRows) {
        items.clear();
        if (newRows != null) {
            String lastDate = null;
            for (DailyReportViewModel.Row row : newRows) {
                if (!row.date.equals(lastDate)) {
                    items.add(new HeaderItem(row.date));
                    lastDate = row.date;
                }
                items.add(row);
            }
        }
        notifyDataSetChanged();
    }

    public void setContext(android.content.Context context) {
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof HeaderItem ? TYPE_HEADER : TYPE_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_report_header, parent, false));
        }
        return new ReportHolder(inflater.inflate(R.layout.item_daily_report, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind((HeaderItem) items.get(position));
        } else {
            ((ReportHolder) holder).bind((DailyReportViewModel.Row) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // For sticky header decoration
    public long getHeaderId(int position) {
        if (position >= items.size()) return -1;
        Object item = items.get(position);
        if (item instanceof DailyReportViewModel.Row) {
            return ((DailyReportViewModel.Row) item).date.hashCode();
        } else if (item instanceof HeaderItem) {
            return ((HeaderItem) item).date.hashCode();
        }
        return -1;
    }

    public View getHeaderView(int position) {
        if (context == null) return null;
        long headerId = getHeaderId(position);
        for (int i = position; i >= 0; i--) {
            if (items.get(i) instanceof HeaderItem) {
                HeaderItem header = (HeaderItem) items.get(i);
                if (header.date.hashCode() == headerId) {
                    View headerView = LayoutInflater.from(context).inflate(R.layout.item_report_header, null);
                    TextView tv = headerView.findViewById(R.id.headerDateText);
                    tv.setText(header.date);
                    return headerView;
                }
            }
        }
        return null;
    }

    static class HeaderItem {
        final String date;
        HeaderItem(String date) { this.date = date; }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView dateText;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.headerDateText);
        }

        void bind(HeaderItem item) {
            dateText.setText(item.date);
        }
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

        void bind(DailyReportViewModel.Row row) {
            itemView.setBackgroundResource(getBindingAdapterPosition() % 2 == 0 ? R.color.row_even : R.color.row_odd);
            date.setText(row.date);
            product.setText(row.productName);
            qty.setText(String.valueOf(row.quantity));
            net.setText(CurrencyUtils.formatRp(row.netSales));
            discount.setText(CurrencyUtils.formatRp(row.discount));
        }
    }
}