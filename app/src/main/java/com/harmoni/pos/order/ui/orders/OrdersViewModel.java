package com.harmoni.pos.order.ui.orders;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.Settlement;
import com.harmoni.pos.order.data.remote.ApiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersViewModel extends ViewModel {

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>();

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<List<Order>> getOrders() {
        return orders;
    }

    public void loadOrders() {
        loading.setValue(true);
        ApiClient.orderService().dailyOrders().enqueue(new Callback<ApiResponse<List<Order>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Order>>> call, Response<ApiResponse<List<Order>>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    orders.setValue(response.body().getData());
                    error.setValue("");
                } else {
                    error.setValue("Failed to load orders: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Order>>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
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
