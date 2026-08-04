package com.harmoni.pos.order.ui.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.util.CurrencyUtils;
import com.harmoni.pos.order.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderHolder> {

    public interface OnDetailClickListener {
        void onDetailClick(Order order);
    }

    private final List<Order> orders = new ArrayList<>();
    private final OnDetailClickListener listener;

    public OrderAdapter(OnDetailClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Order> newOrders) {
        orders.clear();
        if (newOrders != null) {
            orders.addAll(newOrders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order, position % 2 == 0);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class OrderHolder extends RecyclerView.ViewHolder {
        final TextView orderNo, date, customer, payment, status, subTotal, discount, total;

        OrderHolder(@NonNull View itemView) {
            super(itemView);
            orderNo = itemView.findViewById(R.id.orderNoText);
            date = itemView.findViewById(R.id.dateText);
            customer = itemView.findViewById(R.id.customerText);
            payment = itemView.findViewById(R.id.paymentText);
            status = itemView.findViewById(R.id.statusText);
            subTotal = itemView.findViewById(R.id.subTotalText);
            discount = itemView.findViewById(R.id.discountText);
            total = itemView.findViewById(R.id.totalText);
        }

        void bind(Order order, boolean evenRow) {
            itemView.setBackgroundResource(evenRow ? R.color.row_even : R.color.row_odd);
            orderNo.setText(order.getOrderNo());
            date.setText(TimeUtils.toJakarta(order.getCreatedAt()));
            customer.setText(order.getCustomerName());
            payment.setText(order.getPaymentName());
            status.setText(order.getStatus());
            status.setTextColor(itemView.getContext().getColor(statusColor(order)));
            subTotal.setText(CurrencyUtils.formatRp(order.getSubTotal()));
            discount.setText(CurrencyUtils.formatRp(order.getDiscountTotal()));
            total.setText(CurrencyUtils.formatRp(order.getGrandTotal()));
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDetailClick(order);
            });
        }

        private int statusColor(Order order) {
            if (order.isPaid()) return R.color.status_paid;
            if (order.isVoid()) return R.color.status_void;
            return R.color.status_confirmed;
        }
    }
}
