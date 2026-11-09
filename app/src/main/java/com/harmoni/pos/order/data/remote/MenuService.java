package com.harmoni.pos.order.data.remote;

import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.Category;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.ProductCustomization;
import com.harmoni.pos.order.data.model.ProductPageResponse;
import com.harmoni.pos.order.data.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MenuService {

    @GET("api/v1/category/tier")
    Call<ApiResponse<List<Category>>> categories();

    @GET("api/v1/product/category/{id}/price")
    Call<ApiResponse<List<Product>>> productsByCategory(@Path("id") int categoryId);

    @GET("api/v1/product/category/{id}/price/page")
    Call<ApiResponse<ProductPageResponse>> productsByCategoryPage(
            @Path("id") int categoryId,
            @Query("limit") int limit,
            @Query("beforeId") Integer beforeId);

    @GET("api/v1/product/{id}/customization")
    Call<ApiResponse<List<ProductCustomization>>> productCustomizations(@Path("id") int productId);

    @GET("api/v1/user/{username}")
    Call<ApiResponse<User>> getUser(@Path("username") String username);
}
