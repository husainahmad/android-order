package com.harmoni.pos.order.ui.orderform;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.data.model.Customer;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.OrderDetailRequest;
import com.harmoni.pos.order.data.model.OrderDetailSkuRequest;
import com.harmoni.pos.order.data.model.OrderRequest;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.ProductCustomization;
import com.harmoni.pos.order.data.model.SelectedCustomization;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.data.remote.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderFormViewModel extends ViewModel {

    public interface CustomizationCallback {
        void onSuccess(List<ProductCustomization> data);
        void onError(String message);
    }

    public static class OrderTab {
        public final int id;
        public final String label;
        public final List<CartItem> items = new ArrayList<>();
        public String customer = "";
        public Customer customerRecord;
        public String discount = "0";
        public String remark = "";
        public String idempotencyKey = UUID.randomUUID().toString();

        OrderTab(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public void rotateIdempotencyKey() {
            idempotencyKey = UUID.randomUUID().toString();
        }
    }

    private final LinkedHashMap<Integer, OrderTab> tabs = new LinkedHashMap<>();
    private final MutableLiveData<List<OrderTab>> tabsLive = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> activeTabLive = new MutableLiveData<>(-1);
    private final MutableLiveData<List<CartItem>> cart = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Order> confirmedOrder = new MutableLiveData<>();
    private final Map<Integer, List<ProductCustomization>> customizationsByProductId = new HashMap<>();
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
        if (tab != null && !value.equals(tab.customer)) {
            tab.customer = value;
            // Typing a name by hand detaches the order from a registered customer: the
            // id would otherwise point at someone the cashier just renamed over. A
            // customer with no name is matched by phone instead, which is what the cart
            // displays for such a record.
            if (tab.customerRecord != null) {
                String typed = value == null ? "" : value.trim();
                boolean stillNamed = !tab.customerRecord.getNameValue().isEmpty()
                        && tab.customerRecord.getNameValue().equals(typed);
                boolean identifiedByPhone = tab.customerRecord.getNameValue().isEmpty()
                        && tab.customerRecord.getPhoneValue().equals(typed);
                if (!stillNamed && !identifiedByPhone) {
                    tab.customerRecord = null;
                }
            }
            invalidatePendingOrder(tab);
        }
    }

    /** Binds a registered customer to the order. The name travels with the id. */
    public void selectCustomer(Customer customer) {
        OrderTab tab = getActiveTab();
        if (tab == null || customer == null) return;
        tab.customerRecord = customer;
        tab.customer = customer.getNameValue().isEmpty()
                ? customer.getPhoneValue() : customer.getNameValue();
        invalidatePendingOrder(tab);
    }

    /** Returns the order to a walk-in: no id, no name. */
    public void clearCustomer() {
        OrderTab tab = getActiveTab();
        if (tab == null) return;
        tab.customerRecord = null;
        tab.customer = "";
        invalidatePendingOrder(tab);
    }

    /**
     * The registered customer the order can be filed under, given the name the till
     * currently shows. Returns null when the name was edited after picking someone.
     */
    public Customer getLinkedCustomer(String typedName) {
        OrderTab tab = getActiveTab();
        if (tab == null || tab.customerRecord == null) return null;
        String typed = typedName == null ? "" : typedName.trim();
        Customer record = tab.customerRecord;
        boolean stillNamed = !record.getNameValue().isEmpty()
                && record.getNameValue().equals(typed);
        boolean identifiedByPhone = record.getNameValue().isEmpty()
                && record.getPhoneValue().equals(typed);
        return (stillNamed || identifiedByPhone) ? record : null;
    }

    public void setDiscount(String value) {
        OrderTab tab = getActiveTab();
        if (tab != null && !value.equals(tab.discount)) {
            tab.discount = value;
            invalidatePendingOrder(tab);
        }
    }

    public void setRemark(String value) {
        OrderTab tab = getActiveTab();
        if (tab != null && !value.equals(tab.remark)) {
            tab.remark = value;
            invalidatePendingOrder(tab);
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

    /** Resolves and memoises a product's customization groups. */
    public void loadCustomizations(int productId, CustomizationCallback callback) {
        List<ProductCustomization> cached = customizationsByProductId.get(productId);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }
        ApiClient.menuService().productCustomizations(productId)
                .enqueue(new Callback<ApiResponse<List<ProductCustomization>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<ProductCustomization>>> call,
                                           Response<ApiResponse<List<ProductCustomization>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            List<ProductCustomization> data = response.body().getData();
                            customizationsByProductId.put(productId, data);
                            callback.onSuccess(data);
                        } else {
                            callback.onError("HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<ProductCustomization>>> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void addToCart(Product product, Sku sku) {
        addToCart(product, sku, new ArrayList<>(), 1);
    }

    public void addToCart(Product product, Sku sku, List<SelectedCustomization> customizations) {
        addToCart(product, sku, customizations, 1);
    }

    public void addToCart(Product product, Sku sku,
                          List<SelectedCustomization> customizations, int quantity) {
        addToCart(product, sku, customizations, quantity, null);
    }

    public void addToCart(Product product, Sku sku,
                          List<SelectedCustomization> customizations, int quantity,
                          String remark) {
        OrderTab tab = getActiveTab();
        if (tab == null) {
            return;
        }
        int qty = Math.max(1, quantity);
        String note = normalizeRemark(remark);
        List<Integer> optionIds = new ArrayList<>();
        if (customizations != null) {
            for (SelectedCustomization c : customizations) {
                optionIds.addAll(c.getRepeatedOptionIds());
            }
        }
        String key = CartItem.lineKey(product.getId(), sku.getId(), optionIds, note);
        for (CartItem item : tab.items) {
            if (key.equals(item.getLineKey())) {
                item.addQuantity(qty);
                invalidatePendingOrder(tab);
                publishCart();
                return;
            }
        }
        tab.items.add(new CartItem(product.getId(), product.getName(),
                product.getCategoryId(), sku.getId(), sku.getName(),
                sku.getPrice(), qty, customizations, note));
        invalidatePendingOrder(tab);
        publishCart();
    }

    /** Trims a kitchen note; whitespace-only input means no note. */
    private static String normalizeRemark(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public void increment(int productId, int skuId) {
        OrderTab tab = getActiveTab();
        if (tab == null) {
            return;
        }
        for (CartItem item : tab.items) {
            if (item.getProductId() == productId && item.getSkuId() == skuId) {
                item.addQuantity(1);
                invalidatePendingOrder(tab);
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
        invalidatePendingOrder(tab);
        publishCart();
    }

    public void increment(String lineKey) {
        OrderTab tab = getActiveTab();
        if (tab == null) {
            return;
        }
        for (CartItem item : tab.items) {
            if (lineKey.equals(item.getLineKey())) {
                item.addQuantity(1);
                invalidatePendingOrder(tab);
                break;
            }
        }
        publishCart();
    }

    public void decrement(String lineKey) {
        OrderTab tab = getActiveTab();
        if (tab == null) {
            return;
        }
        CartItem toRemove = null;
        for (CartItem item : tab.items) {
            if (lineKey.equals(item.getLineKey())) {
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
        invalidatePendingOrder(tab);
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
        ApiClient.orderService().confirmOrder(request, tab.idempotencyKey)
                .enqueue(new Callback<ApiResponse<Order>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                        submitting.setValue(false);
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            confirmedOrder.setValue(response.body().getData());
                            error.setValue("");
                        } else {
                            // 401/403 handled globally via SessionManager — suppress local
                            if (response.code() == 401 || response.code() == 403) return;
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

    private OrderRequest buildRequest(OrderTab tab, String customerName, String discount,
                                      String remark, int orderType) {
        // Group by product AND line note. The order service stores the note on
        // order_details, so two SKUs of the same product with different notes must
        // travel as two groups or one instruction would be lost.
        Map<String, List<CartItem>> grouped = new LinkedHashMap<>();
        for (CartItem item : tab.items) {
            String key = item.getProductId() + "|" + item.getRemark();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        List<OrderDetailRequest> details = new ArrayList<>();
        for (List<CartItem> group : grouped.values()) {
            List<OrderDetailSkuRequest> skus = new ArrayList<>();
            for (CartItem item : group) {
                skus.add(new OrderDetailSkuRequest(item.getSkuId(), item.getSkuName(),
                        item.getQuantity(), item.getCustomizationRequests()));
            }
            CartItem first = group.get(0);
            details.add(new OrderDetailRequest(first.getProductId(), first.getProductName(),
                    first.getCategoryId(), skus, first.getRemark()));
        }
        Customer linked = getLinkedCustomer(customerName);
        int customerId = linked == null ? -1 : (int) linked.getId();
        String customerPhone = linked == null ? "" : linked.getPhoneValue();
        return new OrderRequest(orderType, customerName, customerId, customerPhone,
                remark, details, discount);
    }

    /** Any order-affecting edit rotates the idempotency key and drops the stale order. */
    private void invalidatePendingOrder(OrderTab tab) {
        tab.rotateIdempotencyKey();
        if (tab.id == activeId) {
            confirmedOrder.setValue(null);
        }
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
