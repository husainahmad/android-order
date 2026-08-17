package com.harmoni.pos.order.ui.orderpager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.harmoni.pos.order.ui.orderform.OrderFormFragment;
import com.harmoni.pos.order.ui.orders.OrdersFragment;

import java.util.ArrayList;
import java.util.List;

public class OrderPagerAdapter extends FragmentStateAdapter {

    private final List<Integer> orderIds = new ArrayList<>();
    public static final int ORDERS_FRAGMENT_POSITION = 0;
    private static final long ORDERS_FRAGMENT_ID = -1001L;

    public OrderPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void setOrderIds(List<Integer> newIds) {
        if (newIds != null && newIds.equals(orderIds)) {
            return;
        }
        orderIds.clear();
        if (newIds != null) {
            orderIds.addAll(newIds);
        }
        notifyDataSetChanged();
    }

    public int getOrderIdAtPosition(int position) {
        if (position <= 0 || position > orderIds.size()) {
            return -1;
        }
        return orderIds.get(position - 1);
    }

    public int getPositionForOrderId(int orderId) {
        int index = orderIds.indexOf(orderId);
        return index != -1 ? index + 1 : -1;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == ORDERS_FRAGMENT_POSITION) {
            return new OrdersFragment();
        }
        int orderId = orderIds.get(position - 1);
        return OrderFormFragment.newInstance(orderId);
    }

    @Override
    public int getItemCount() {
        return orderIds.size() + 1;
    }

    @Override
    public long getItemId(int position) {
        if (position == ORDERS_FRAGMENT_POSITION) {
            return ORDERS_FRAGMENT_ID;
        }
        return orderIds.get(position - 1);
    }

    @Override
    public boolean containsItem(long itemId) {
        if (itemId == ORDERS_FRAGMENT_ID) {
            return true;
        }
        return orderIds.contains((int) itemId);
    }
}
