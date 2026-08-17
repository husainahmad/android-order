package com.harmoni.pos.order.ui.orderform;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.OrderDetailRequest;
import com.harmoni.pos.order.data.model.OrderDetailSkuRequest;
import com.harmoni.pos.order.data.model.OrderRequest;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.data.remote.ApiClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderFormViewModel extends ViewModel {

    public static class OrderTab {
        public final int id;
        public final String label;
        public final List<CartItem> items = new ArrayList<>();
        public String customer = "";
        public String discount = "0";
        public String remark = "";

        OrderTab(int id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    private final LinkedHashMap<Integer, OrderTab> tabs = new LinkedHashMap<>();
    private final MutableLiveData<List<OrderTab>> tabsLive = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> activeTabLive = new MutableLiveData<>(-1);
    private final MutableLiveData<List<CartItem>> cart = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Order> confirmedOrder = new MutableLiveData<>();
    private int sequence = 0;
    private int activeId = -1;

    public LiveData<List<OrderTab>> getTabs() {
        return tabsLive;
    }

    public LiveData<Integer> getActiveTabId() {
        return activeTabLive;
    }

    public LiveData<List<CartItem>> getCart() {
        return cart;
    }

    public LiveData<Boolean> getSubmitting() {
        return submitting;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Order> getConfirmedOrder() {
        return confirmedOrder;
    }

    public OrderTab getActiveTab() {
        return tabs.get(activeId);
    }

    public void ensureActiveTab() {
        if (tabs.isEmpty()) {
            createNewTab();
        } else if (tabs.get(activeId) == null) {
            activeId = tabs.keySet().iterator().next();
            publishActive();
            publishCart();
        }
    }

    public int createNewTab() {
        sequence++;
        OrderTab tab = new OrderTab(sequence, String.format(Locale.US, "Order#%03d", sequence));
        tabs.put(tab.id, tab);
        activeId = tab.id;
        error.setValue("");
        publishTabs();
        publishActive();
        publishCart();
        return tab.id;
    }

    public OrderTab getTab(int id) {
        return tabs.get(id);
    }

    public void switchTab(int id) {
        if (tabs.containsKey(id)) {
            activeId = id;
            error.setValue("");
            publishActive();
            publishCart();
        }
    }

    public int removeActiveTab() {
        return removeTab(activeId);
    }

    public int removeTab(int id) {
        if (!tabs.containsKey(id)) {
            return -1;
        }
        List<Integer> ids = new ArrayList<>(tabs.keySet());
        int index = ids.indexOf(id);
        tabs.remove(id);
        confirmedOrder.setValue(null);
        error.setValue("");
        if (tabs.isEmpty()) {
            activeId = -1;
            publishTabs();
            publishActive();
            publishCart();
            return -1;
        }
        ids = new ArrayList<>(tabs.keySet());
        activeId = ids.get(Math.min(index, ids.size() - 1));
        publishTabs();
        publishActive();
        publishCart();
        return activeId;
    }

    public void resetConfirmedOrder() {
        confirmedOrder.setValue(null);
    }

    public void setCustomer(String value) {
        OrderTab tab = getActiveTab();
        if (tab != null) {
            tab.customer = value;
        }
    }

    public void setDiscount(String value) {
        OrderTab tab = getActiveTab();
        if (tab != null) {
            tab.discount = value;
        }
    }

    public void setRemark(String value) {
        OrderTab tab = getActiveTab();
        if (tab != null) {
            tab.remark = value;
        }
    }

    public double getActiveDiscount() {
        OrderTab tab = getActiveTab();
        if (tab == null) {
            return 0;
        }
        try {
            return Double.parseDouble(tab.discount.isEmpty() ? "0" : tab.discount);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void addToCart(Product product, Sku sku) {
        OrderTab tab = getActiveTab();
        if (tab == null) {
            return;
        }
        boolean found = false;
        for (CartItem item : tab.items) {
            if (item.getProductId() == product.getId() && item.getSkuId() == sku.getId()) {
                item.addQuantity(1);
                found = true;
                break;
            }
        }
        if (!found) {
            tab.items.add(new CartItem(product.getId(), product.getName(),
                    product.getCategoryId(), sku.getId(), sku.getName(),
                    sku.getPrice(), 1));
        }
        publishCart();
    }

    public void increment(int productId, int skuId) {
        OrderTab tab = getActiveTab();
        if (tab == null) {
            return;
        }
        for (CartItem item : tab.items) {
            if (item.getProductId() == productId && item.getSkuId() == skuId) {
                item.addQuantity(1);
                break;
            }
        }
        publishCart();
    }

    public void decrement(int productId, int skuId) {
        OrderTab tab = getActiveTab();
        if (tab == null) {
            return;
        }
        CartItem toRemove = null;
        for (CartItem item : tab.items) {
            if (item.getProductId() == productId && item.getSkuId() == skuId) {
                item.addQuantity(-1);
                if (item.getQuantity() <= 0) {
                    toRemove = item;
                }
                break;
            }
        }
        if (toRemove != null) {
            tab.items.remove(toRemove);
        }
        publishCart();
    }

    public double getSubtotal() {
        OrderTab tab = getActiveTab();
        if (tab == null) {
            return 0;
        }
        double total = 0;
        for (CartItem item : tab.items) {
            total += item.getLineTotal();
        }
        return total;
    }

    public void confirmOrder(String customerName, String discount, String remark, int orderType) {
        OrderTab tab = getActiveTab();
        if (tab == null || tab.items.isEmpty()) {
            error.setValue("Cart is empty");
            return;
        }
        submitting.setValue(true);
        OrderRequest request = buildRequest(tab, customerName, discount, remark, orderType);
        ApiClient.orderService().confirmOrder(request).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                submitting.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    confirmedOrder.setValue(response.body().getData());
                } else {
                    error.setValue("Confirm order failed: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Order>> call, Throwable t) {
                submitting.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    private OrderRequest buildRequest(OrderTab tab, String customerName, String discount, String remark, int orderType) {
        Map<Integer, List<CartItem>> grouped = new LinkedHashMap<>();
        for (CartItem item : tab.items) {
            grouped.computeIfAbsent(item.getProductId(), k -> new ArrayList<>()).add(item);
        }
        List<OrderDetailRequest> details = new ArrayList<>();
        for (List<CartItem> group : grouped.values()) {
            List<OrderDetailSkuRequest> skus = new ArrayList<>();
            for (CartItem item : group) {
                skus.add(new OrderDetailSkuRequest(item.getSkuId(), item.getSkuName(), item.getQuantity()));
            }
            CartItem first = group.get(0);
            details.add(new OrderDetailRequest(first.getProductId(), first.getProductName(),
                    first.getCategoryId(), skus));
        }
        return new OrderRequest(orderType, customerName, -1, remark, details, discount);
    }

    private void publishTabs() {
        tabsLive.setValue(new ArrayList<>(tabs.values()));
    }

    private void publishActive() {
        activeTabLive.setValue(activeId);
    }

    private void publishCart() {
        OrderTab tab = getActiveTab();
        List<CartItem> copy = new ArrayList<>();
        if (tab != null) {
            for (CartItem item : tab.items) {
                copy.add(item.copy());
            }
        }
        cart.setValue(copy);
    }
}
