package com.harmoni.pos.order.ui.orderform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfirmItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_SKU = 1;

    private final List<Object> display = new ArrayList<>();

    public void submitList(List<CartItem> newItems) {
        display.clear();
        if (newItems != null) {
            Map<Integer, List<CartItem>> groups = new LinkedHashMap<>();
            for (CartItem item : newItems) {
                groups.computeIfAbsent(item.getProductId(), k -> new ArrayList<>()).add(item);
            }
            for (List<CartItem> group : groups.values()) {
                display.add(new ProductHeader(group.get(0).getProductName()));
                display.addAll(group);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return display.get(position) instanceof CartItem ? VIEW_SKU : VIEW_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_confirm_header, parent, false));
        }
        return new SkuHolder(inflater.inflate(R.layout.item_confirm_sku, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).productName.setText(((ProductHeader) display.get(position)).productName);
        } else {
            CartItem item = (CartItem) display.get(position);
            SkuHolder skuHolder = (SkuHolder) holder;
            skuHolder.skuName.setText(item.getSkuName());
            skuHolder.skuPrice.setText(CurrencyUtils.formatRp(item.getPrice()));
            skuHolder.quantity.setText("x" + item.getQuantity());
            skuHolder.lineTotal.setText(CurrencyUtils.formatRp(item.getLineTotal()));
        }
    }

    @Override
    public int getItemCount() {
        return display.size();
    }

    static class ProductHeader {
        final String productName;
        ProductHeader(String productName) {
            this.productName = productName;
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView productName;
        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productNameText);
        }
    }

    static class SkuHolder extends RecyclerView.ViewHolder {
        final TextView skuName;
        final TextView skuPrice;
        final TextView quantity;
        final TextView lineTotal;
        SkuHolder(@NonNull View itemView) {
            super(itemView);
            skuName = itemView.findViewById(R.id.skuNameText);
            skuPrice = itemView.findViewById(R.id.skuPriceText);
            quantity = itemView.findViewById(R.id.quantityText);
            lineTotal = itemView.findViewById(R.id.lineTotalText);
        }
    }
}
