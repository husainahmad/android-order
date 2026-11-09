package com.harmoni.pos.order.ui.orders;

import android.content.Context;
import android.content.res.ColorStateList;
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
        holder.bind(getItem(position));
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

        void bind(Order order) {
            Context context = itemView.getContext();

            orderNo.setText(order.getOrderNo());
            date.setText(TimeUtils.toJakarta(order.getCreatedAt()));
            customer.setText(order.getCustomerName().trim().isEmpty()
                    ? context.getString(R.string.walk_in) : order.getCustomerName());
            type.setText(typeofLabel(context, order));

            bindPayment(context, order);
            bindStatus(context, order);

            subTotal.setText(CurrencyUtils.formatRpOrders(order.getSubTotal()));
            discount.setText(CurrencyUtils.formatRpOrders(order.getDiscountTotal()));
            discount.setTextColor(context.getColor(order.getDiscountTotal() > 0
                    ? R.color.status_void : R.color.text_secondary));
            total.setText(CurrencyUtils.formatRpOrders(order.getGrandTotal()));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDetailClick(order);
            });
        }

        private void bindPayment(Context context, Order order) {
            String method = order.getPaymentName();
            boolean unpaid = "UNPAID".equalsIgnoreCase(method);
            String label;
            if (unpaid) {
                label = context.getString(R.string.payment_unpaid);
            } else if ("CASH".equalsIgnoreCase(method)) {
                label = context.getString(R.string.payment_cash);
            } else if ("CARD".equalsIgnoreCase(method)) {
                label = context.getString(R.string.payment_card);
            } else if ("QR".equalsIgnoreCase(method) || "QRIS".equalsIgnoreCase(method)) {
                label = context.getString(R.string.payment_qris);
            } else {
                label = method;
            }
            payment.setText(label);
            payment.setTextColor(context.getColor(unpaid
                    ? R.color.status_confirmed : R.color.text_primary));
            payment.setBackgroundTintList(ColorStateList.valueOf(context.getColor(unpaid
                    ? R.color.payment_unpaid_bg : R.color.payment_badge_bg)));
        }

        private void bindStatus(Context context, Order order) {
            int bg;
            int fg;
            if (order.isPaid()) {
                bg = R.color.status_paid_bg;
                fg = R.color.status_paid;
            } else if (order.isVoid()) {
                bg = R.color.status_void_bg;
                fg = R.color.status_void;
            } else if (order.isConfirmed()) {
                bg = R.color.status_confirmed_bg;
                fg = R.color.status_confirmed;
            } else {
                bg = R.color.payment_badge_bg;
                fg = R.color.text_secondary;
            }
            status.setText(order.getStatus());
            status.setTextColor(context.getColor(fg));
            status.setBackgroundTintList(ColorStateList.valueOf(context.getColor(bg)));
        }

        private String typeofLabel(Context context, Order order) {
            return "Takeaway".equalsIgnoreCase(order.getServiceTypeName())
                    ? context.getString(R.string.takeaway)
                    : context.getString(R.string.dine_in);
        }
    }
}
