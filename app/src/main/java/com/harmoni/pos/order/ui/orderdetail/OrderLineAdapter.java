package com.harmoni.pos.order.ui.orderdetail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.OrderCustomizationLine;
import com.harmoni.pos.order.data.model.OrderDetail;
import com.harmoni.pos.order.data.model.OrderDetailSku;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

/** Items breakdown for the order detail dialog, with each line's customization choices. */
public class OrderLineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SKU = 1;
    private static final int TYPE_CUSTOM = 2;

    private static class Header { final String name; Header(String name) { this.name = name; } }
    private static class SkuRow { final OrderDetailSku sku; SkuRow(OrderDetailSku sku) { this.sku = sku; } }
    private static class CustomRow { final OrderCustomizationLine line; CustomRow(OrderCustomizationLine line) { this.line = line; } }

    private final List<Object> rows = new ArrayList<>();

    public void submit(Order order) {
        rows.clear();
        if (order != null && order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail == null) continue;
                String note = detail.getRemark();
                String header = note.isEmpty()
                        ? detail.getProductName()
                        : detail.getProductName() + "\nNote: " + note;
                rows.add(new Header(header));
                for (OrderDetailSku sku : detail.getOrderDetailSkus()) {
                    if (sku == null) continue;
                    rows.add(new SkuRow(sku));
                    for (OrderCustomizationLine line : detail.getCustomizationsForSku(sku)) {
                        rows.add(new CustomRow(line));
                    }
                }
                for (OrderCustomizationLine line : detail.getUnattachedCustomizations()) {
                    rows.add(new CustomRow(line));
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object row = rows.get(position);
        if (row instanceof Header) return TYPE_HEADER;
        if (row instanceof SkuRow) return TYPE_SKU;
        return TYPE_CUSTOM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_order_line_header, parent, false));
        }
        if (viewType == TYPE_SKU) {
            return new SkuHolder(inflater.inflate(R.layout.item_order_line_sku, parent, false));
        }
        return new CustomHolder(inflater.inflate(R.layout.item_order_line_customization, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).name.setText(((Header) row).name);
        } else if (holder instanceof SkuHolder) {
            OrderDetailSku sku = ((SkuRow) row).sku;
            SkuHolder h = (SkuHolder) holder;
            h.name.setText(sku.getSkuName());
            h.qty.setText("x" + (int) sku.getQuantity());
            h.amount.setText(CurrencyUtils.formatRp(sku.getAmount()));
        } else {
            OrderCustomizationLine line = ((CustomRow) row).line;
            CustomHolder h = (CustomHolder) holder;
            h.name.setText(line.getGroupedDisplayName());
            h.amount.setText(line.getAmount() > 0
                    ? "+" + CurrencyUtils.formatRp(line.getAmount()) : "");
        }
    }

    @Override
    public int getItemCount() { return rows.size(); }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView name;
        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.lineProductName);
        }
    }

    static class SkuHolder extends RecyclerView.ViewHolder {
        final TextView name, qty, amount;
        SkuHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.lineSkuName);
            qty = itemView.findViewById(R.id.lineSkuQty);
            amount = itemView.findViewById(R.id.lineSkuAmount);
        }
    }

    static class CustomHolder extends RecyclerView.ViewHolder {
        final TextView name, amount;
        CustomHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.lineCustomName);
            amount = itemView.findViewById(R.id.lineCustomAmount);
        }
    }
}
