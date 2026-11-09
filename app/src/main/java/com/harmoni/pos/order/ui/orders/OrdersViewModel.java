package com.harmoni.pos.order.ui.orders;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.OrderPageResponse;
import com.harmoni.pos.order.data.model.Settlement;
import com.harmoni.pos.order.data.remote.ApiClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersViewModel extends ViewModel {

    private static final int PAGE_SIZE = 50;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>();

    private Integer nextCursor;

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getLoadingMore() { return loadingMore; }
    public LiveData<Boolean> getHasMore() { return hasMore; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<Order>> getOrders() { return orders; }

    /** Loads the first page, replacing whatever is on screen. */
    public void loadOrders() {
        error.setValue("");
        loading.setValue(true);
        ApiClient.orderService().dailyOrdersPage(PAGE_SIZE, null)
                .enqueue(new Callback<ApiResponse<OrderPageResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<OrderPageResponse>> call,
                                           Response<ApiResponse<OrderPageResponse>> response) {
                        loading.setValue(false);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            OrderPageResponse page = response.body().getData();
                            nextCursor = page.getNextCursor();
                            hasMore.setValue(page.hasMore());
                            orders.setValue(page.getOrders());
                            error.setValue("");
                        } else {
                            if (response.code() == 401 || response.code() == 403) return;
                            error.setValue("Failed to load orders: HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<OrderPageResponse>> call, Throwable t) {
                        loading.setValue(false);
                        error.setValue("Network error: " + t.getMessage());
                    }
                });
    }

    /** Appends the next page. A no-op at the end of the list or while a page is in flight. */
    public void loadMore() {
        Integer cursor = nextCursor;
        if (!Boolean.TRUE.equals(hasMore.getValue())
                || Boolean.TRUE.equals(loadingMore.getValue())
                || Boolean.TRUE.equals(loading.getValue())
                || cursor == null) {
            return;
        }
        loadingMore.setValue(true);
        ApiClient.orderService().dailyOrdersPage(PAGE_SIZE, cursor)
                .enqueue(new Callback<ApiResponse<OrderPageResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<OrderPageResponse>> call,
                                           Response<ApiResponse<OrderPageResponse>> response) {
                        loadingMore.setValue(false);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            OrderPageResponse page = response.body().getData();
                            List<Order> current = orders.getValue();
                            List<Order> merged = current == null
                                    ? new ArrayList<>() : new ArrayList<>(current);
                            Set<Integer> known = new HashSet<>();
                            for (Order order : merged) known.add(order.getId());
                            for (Order order : page.getOrders()) {
                                if (!known.contains(order.getId())) merged.add(order);
                            }
                            nextCursor = page.getNextCursor();
                            hasMore.setValue(page.hasMore());
                            orders.setValue(merged);
                        } else {
                            if (response.code() == 401 || response.code() == 403) return;
                            error.setValue("Failed to load more: HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<OrderPageResponse>> call, Throwable t) {
                        loadingMore.setValue(false);
                        // Leave the cursor unadvanced so scrolling again retries.
                        error.setValue("Failed to load more: " + t.getMessage());
                    }
                });
    }

    /** Reloads the first page after a change, keeping the current rows on screen. */
    public void refresh() {
        loadOrders();
    }

    public void loadSettlement(LoadCallback<Settlement> callback) {
        String today = com.harmoni.pos.order.util.TimeUtils.todayDate();
        String start = today + "T00:00:00";
        String end = today + "T23:59:59";
        ApiClient.orderService().settlement(start, end).enqueue(new Callback<ApiResponse<Settlement>>() {
            @Override
            public void onResponse(Call<ApiResponse<Settlement>> call, Response<ApiResponse<Settlement>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    if (response.code() == 401 || response.code() == 403) return;
                    callback.onError("Failed to load settlement: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Settlement>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public interface LoadCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}
