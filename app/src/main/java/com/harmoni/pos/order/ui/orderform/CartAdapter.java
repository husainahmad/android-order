package com.harmoni.pos.order.ui.orderform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CartAdapter extends ListAdapter<Object, RecyclerView.ViewHolder> {

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_SKU = 1;

    public interface OnQuantityChangeListener {
        void onIncrement(int productId, int skuId);
        void onDecrement(int productId, int skuId);
    }

    private final OnQuantityChangeListener listener;

    public CartAdapter(OnQuantityChangeListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Object> DIFF = new DiffUtil.ItemCallback<Object>() {
        @Override
        public boolean areItemsTheSame(@NonNull Object o1, @NonNull Object o2) {
            if (o1 instanceof CartItem && o2 instanceof CartItem) {
                CartItem a = (CartItem) o1;
                CartItem b = (CartItem) o2;
                return a.getProductId() == b.getProductId() && a.getSkuId() == b.getSkuId();
            }
            if (o1 instanceof CartHeader && o2 instanceof CartHeader) {
                return Objects.equals(((CartHeader) o1).productName, ((CartHeader) o2).productName);
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Object o1, @NonNull Object o2) {
            if (o1 instanceof CartItem && o2 instanceof CartItem) {
                CartItem a = (CartItem) o1;
                CartItem b = (CartItem) o2;
                return a.getQuantity() == b.getQuantity()
                        && Objects.equals(a.getSkuName(), b.getSkuName())
                        && Objects.equals(a.getLineTotal(), b.getLineTotal());
            }
            return true;
        }
    };

    private static class CartHeader {
        final String productName;

        CartHeader(String productName) {
            this.productName = productName;
        }
    }

    private static List<Object> buildDisplay(List<CartItem> newItems) {
        List<Object> display = new ArrayList<>();
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
        return display;
    }

    public void submitItems(List<CartItem> newItems) {
        super.submitList(buildDisplay(newItems));
    }

    public void submitItems(List<CartItem> newItems, Runnable commitCallback) {
        super.submitList(buildDisplay(newItems), commitCallback);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) instanceof CartItem ? VIEW_SKU : VIEW_HEADER;
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
        Object item = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).productName.setText(((CartHeader) item).productName);
        } else {
            CartItem ci = (CartItem) item;
            SkuHolder skuHolder = (SkuHolder) holder;
            skuHolder.skuName.setText(ci.getSkuName());
            skuHolder.quantity.setText(String.valueOf(ci.getQuantity()));
            skuHolder.lineTotal.setText(CurrencyUtils.formatRp(ci.getLineTotal()));
            skuHolder.minus.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                listener.onDecrement(ci.getProductId(), ci.getSkuId());
            });
            skuHolder.plus.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                listener.onIncrement(ci.getProductId(), ci.getSkuId());
            });
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
