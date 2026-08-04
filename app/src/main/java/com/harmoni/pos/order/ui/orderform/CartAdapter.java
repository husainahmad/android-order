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

public class CartAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_SKU = 1;

    public interface OnQuantityChangeListener {
        void onIncrement(int productId, int skuId);
        void onDecrement(int productId, int skuId);
    }

    private final List<Object> display = new ArrayList<>();
    private final OnQuantityChangeListener listener;

    public CartAdapter(OnQuantityChangeListener listener) {
        this.listener = listener;
    }

    private static class CartHeader {
        final String productName;

        CartHeader(String productName) {
            this.productName = productName;
        }
    }

    public void submitList(List<CartItem> newItems) {
        display.clear();
        if (newItems != null) {
            Map<Integer, List<CartItem>> groups = new LinkedHashMap<>();
            for (CartItem item : newItems) {
                groups.computeIfAbsent(item.getProductId(), k -> new ArrayList<>()).add(item);
            }
            for (List<CartItem> group : groups.values()) {
                display.add(new CartHeader(group.get(0).getProductName()));
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
            return new HeaderHolder(inflater.inflate(R.layout.item_cart_header, parent, false));
        }
        return new SkuHolder(inflater.inflate(R.layout.item_cart, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).productName.setText(((CartHeader) display.get(position)).productName);
        } else {
            CartItem item = (CartItem) display.get(position);
            SkuHolder skuHolder = (SkuHolder) holder;
            skuHolder.skuName.setText(item.getSkuName());
            skuHolder.quantity.setText(String.valueOf(item.getQuantity()));
            skuHolder.lineTotal.setText(CurrencyUtils.formatRp(item.getLineTotal()));
            skuHolder.minus.setOnClickListener(v -> listener.onDecrement(item.getProductId(), item.getSkuId()));
            skuHolder.plus.setOnClickListener(v -> listener.onIncrement(item.getProductId(), item.getSkuId()));
        }
    }

    @Override
    public int getItemCount() {
        return display.size();
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
        final TextView quantity;
        final TextView lineTotal;
        final View minus;
        final View plus;

        SkuHolder(@NonNull View itemView) {
            super(itemView);
            skuName = itemView.findViewById(R.id.skuNameText);
            quantity = itemView.findViewById(R.id.quantityText);
            lineTotal = itemView.findViewById(R.id.lineTotalText);
            minus = itemView.findViewById(R.id.minusButton);
            plus = itemView.findViewById(R.id.plusButton);
        }
    }
}
