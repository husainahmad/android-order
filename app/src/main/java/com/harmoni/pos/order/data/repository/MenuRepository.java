package com.harmoni.pos.order.data.repository;

import android.os.Handler;
import android.os.Looper;

import com.harmoni.pos.order.PosApplication;
import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.Category;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.remote.ApiClient;
import com.harmoni.pos.order.util.JsonCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final Map<Integer, List<Product>> memoryProducts = new HashMap<>();
    private static List<Category> memoryCategories;

    public void getCategories(RepositoryCallback<List<Category>> callback) {
        if (memoryCategories != null) {
            MAIN.post(() -> callback.onSuccess(memoryCategories));
            return;
        }
        EXECUTOR.execute(() -> {
            List<Category> cached = JsonCache.readCategories(PosApplication.getAppContext());
            if (cached != null) {
                memoryCategories = cached;
                MAIN.post(() -> callback.onSuccess(cached));
                return;
            }
            ApiClient.menuService().categories().enqueue(new Callback<ApiResponse<List<Category>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        List<Category> data = response.body().getData();
                        memoryCategories = data;
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
        List<Product> memory = memoryProducts.get(categoryId);
        if (memory != null) {
            MAIN.post(() -> callback.onSuccess(memory));
            return;
        }
        EXECUTOR.execute(() -> {
            List<Product> cached = JsonCache.readProducts(PosApplication.getAppContext(), categoryId);
            if (cached != null) {
                memoryProducts.put(categoryId, cached);
                MAIN.post(() -> callback.onSuccess(cached));
            }
            fetchProducts(categoryId, callback, cached != null);
        });
    }

    private void fetchProducts(int categoryId, RepositoryCallback<List<Product>> callback, boolean silent) {
        ApiClient.menuService().productsByCategory(categoryId).enqueue(new Callback<ApiResponse<List<Product>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Product>>> call, Response<ApiResponse<List<Product>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Product> data = response.body().getData();
                    EXECUTOR.execute(() -> {
                        JsonCache.writeProducts(PosApplication.getAppContext(), categoryId, data);
                        memoryProducts.put(categoryId, data);
                    });
                    callback.onSuccess(data);
                } else if (!silent) {
                    callback.onError(errorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Product>>> call, Throwable t) {
                if (!silent) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }

    /**
     * One cursor page of products for a category. Pages are not cached individually;
     * the first page is written to the disk cache so a cold start still has content.
     */
    public void getProductsPage(int categoryId, Integer beforeId, int limit,
                                RepositoryCallback<com.harmoni.pos.order.data.model.ProductPageResponse> callback) {
        final boolean firstPage = beforeId == null;
        ApiClient.menuService().productsByCategoryPage(categoryId, limit, beforeId)
                .enqueue(new Callback<ApiResponse<com.harmoni.pos.order.data.model.ProductPageResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<com.harmoni.pos.order.data.model.ProductPageResponse>> call,
                                           Response<ApiResponse<com.harmoni.pos.order.data.model.ProductPageResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            com.harmoni.pos.order.data.model.ProductPageResponse page = response.body().getData();
                            if (firstPage) {
                                List<com.harmoni.pos.order.data.model.Product> products = page.getProducts();
                                EXECUTOR.execute(() -> JsonCache.writeProducts(
                                        PosApplication.getAppContext(), categoryId, products));
                            }
                            callback.onSuccess(page);
                        } else {
                            callback.onError(errorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<com.harmoni.pos.order.data.model.ProductPageResponse>> call,
                                          Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void clearCache() {
        EXECUTOR.execute(() -> JsonCache.clear(PosApplication.getAppContext()));
    }

    private static <T> String errorMessage(Response<ApiResponse<T>> response) {
        if (response.code() == 401) return "Session expired – please login again";
        if (response.code() == 403) return "Access denied – you don't have permission (403)";
        return "HTTP " + response.code();
    }
}
