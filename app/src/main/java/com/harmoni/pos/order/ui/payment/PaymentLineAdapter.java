package com.harmoni.pos.order.ui.payment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.OrderDetail;
import com.harmoni.pos.order.data.model.OrderDetailSku;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

/** Compact order summary used by the payment dialog, sourced from the stored order. */
public class PaymentLineAdapter extends RecyclerView.Adapter<PaymentLineAdapter.Holder> {

    private final List<OrderDetail> details = new ArrayList<>();

    public void submit(Order order) {
        details.clear();
        if (order != null && order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail != null) details.add(detail);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_order_line, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(details.get(position));
    }

    @Override
    public int getItemCount() { return details.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name, modifiers, qty, amount;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.itemName);
            modifiers = itemView.findViewById(R.id.itemModifiers);
            qty = itemView.findViewById(R.id.itemQty);
            amount = itemView.findViewById(R.id.itemAmount);
        }

        void bind(OrderDetail detail) {
            name.setText(detail.getProductName());

            List<OrderDetailSku> skus = detail.getOrderDetailSkus();
            int quantity = 0;
            if (skus != null) {
                for (OrderDetailSku sku : skus) {
                    quantity += (int) sku.getQuantity();
                }
            }
            if (quantity <= 0) quantity = 1;

            StringBuilder meta = new StringBuilder();
            if (skus != null && !skus.isEmpty()) {
                OrderDetailSku first = skus.get(0);
                meta.append(first.getSkuName());
            }
            String customizations = detail.getGroupedCustomizationNames();
            if (customizations != null && !customizations.isEmpty()) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append(customizations);
            }
            modifiers.setText(meta.toString());
            qty.setText(itemView.getContext().getString(R.string.qty_format, quantity));
            amount.setText(CurrencyUtils.formatRp(detail.getAmount()));
        }
    }
}
