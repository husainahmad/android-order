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
import com.harmoni.pos.order.data.model.Customer;

public class CustomerAdapter extends ListAdapter<Customer, CustomerAdapter.Holder> {

    public interface OnSelectListener {
        void onSelect(Customer customer);
    }

    private final OnSelectListener listener;

    public CustomerAdapter(OnSelectListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Customer> DIFF = new DiffUtil.ItemCallback<Customer>() {
        @Override
        public boolean areItemsTheSame(@NonNull Customer a, @NonNull Customer b) {
            return a.getId() == b.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Customer a, @NonNull Customer b) {
            return a.getId() == b.getId()
                    && a.getName().equals(b.getName())
                    && a.getPhone().equals(b.getPhone());
        }
    };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Customer customer = getItem(position);
        holder.name.setText(customer.getDisplayName());
        String phone = customer.getPhoneValue();
        holder.phone.setText(phone.isEmpty() ? "" : phone);
        holder.phone.setVisibility(phone.isEmpty() ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelect(customer);
        });
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView phone;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.customerNameText);
            phone = itemView.findViewById(R.id.customerPhoneText);
        }
    }
}
