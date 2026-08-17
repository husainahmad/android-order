package com.harmoni.pos.order.ui.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.util.CurrencyUtils;
import com.harmoni.pos.order.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrderAdapter extends ListAdapter<Order, OrderAdapter.OrderHolder> {

    public interface OnDetailClickListener {
        void onDetailClick(Order order);
    }

    private final OnDetailClickListener listener;

    public OrderAdapter(OnDetailClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Order> DIFF = new DiffUtil.ItemCallback<Order>() {
        @Override
        public boolean areItemsTheSame(@NonNull Order o1, @NonNull Order o2) {
            return Objects.equals(o1.getOrderNo(), o2.getOrderNo());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Order o1, @NonNull Order o2) {
            return Objects.equals(o1.getCreatedAt(), o2.getCreatedAt())
                    && Objects.equals(o1.getCustomerName(), o2.getCustomerName())
                    && Objects.equals(o1.getServiceTypeName(), o2.getServiceTypeName())
                    && Objects.equals(o1.getPaymentName(), o2.getPaymentName())
                    && Objects.equals(o1.getStatus(), o2.getStatus())
                    && Objects.equals(o1.getSubTotal(), o2.getSubTotal())
                    && Objects.equals(o1.getDiscountTotal(), o2.getDiscountTotal())
                    && Objects.equals(o1.getGrandTotal(), o2.getGrandTotal());
        }
    };

    public void submitList(List<Order> newOrders) {
        super.submitList(newOrders == null ? null : new ArrayList<>(newOrders));
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
        holder.bind(getItem(position), position % 2 == 0);
    }

    class OrderHolder extends RecyclerView.ViewHolder {
        final TextView orderNo, date, customer, type, payment, status, subTotal, discount, total;

        OrderHolder(@NonNull View itemView) {
            super(itemView);
            orderNo = itemView.findViewById(R.id.orderNoText);
            date = itemView.findViewById(R.id.dateText);
            customer = itemView.findViewById(R.id.customerText);
            type = itemView.findViewById(R.id.typeText);
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
            type.setText(order.getServiceTypeName());
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
