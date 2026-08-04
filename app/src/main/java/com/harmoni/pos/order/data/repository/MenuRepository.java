package com.harmoni.pos.order.data.repository;

import android.os.Handler;
import android.os.Looper;

import com.harmoni.pos.order.PosApplication;
import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.Category;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.remote.ApiClient;
import com.harmoni.pos.order.util.JsonCache;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuRepository {

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public void getCategories(RepositoryCallback<List<Category>> callback) {
        EXECUTOR.execute(() -> {
            List<Category> cached = JsonCache.readCategories(PosApplication.getAppContext());
            if (cached != null) {
                MAIN.post(() -> callback.onSuccess(cached));
                return;
            }
            ApiClient.menuService().categories().enqueue(new Callback<ApiResponse<List<Category>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        List<Category> data = response.body().getData();
                        EXECUTOR.execute(() -> JsonCache.writeCategories(PosApplication.getAppContext(), data));
                        callback.onSuccess(data);
                    } else {
                        callback.onError(errorMessage(response));
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                    callback.onError(t.getMessage());
                }
            });
        });
    }

    public void getProducts(int categoryId, RepositoryCallback<List<Product>> callback) {
        EXECUTOR.execute(() -> {
            List<Product> cached = JsonCache.readProducts(PosApplication.getAppContext(), categoryId);
            if (cached != null) {
                MAIN.post(() -> callback.onSuccess(cached));
                return;
            }
            ApiClient.menuService().productsByCategory(categoryId).enqueue(new Callback<ApiResponse<List<Product>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Product>>> call, Response<ApiResponse<List<Product>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        List<Product> data = response.body().getData();
                        EXECUTOR.execute(() -> JsonCache.writeProducts(PosApplication.getAppContext(), categoryId, data));
                        callback.onSuccess(data);
                    } else {
                        callback.onError(errorMessage(response));
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Product>>> call, Throwable t) {
                    callback.onError(t.getMessage());
                }
            });
        });
    }

    public void clearCache() {
        EXECUTOR.execute(() -> JsonCache.clear(PosApplication.getAppContext()));
    }

    private static <T> String errorMessage(Response<ApiResponse<T>> response) {
        if (response.code() == 401) return "Unauthorized";
        return "HTTP " + response.code();
    }
}
