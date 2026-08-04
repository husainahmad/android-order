package com.harmoni.pos.order.data.remote;

import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.Category;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface MenuService {

    @GET("api/v1/category/tier")
    Call<ApiResponse<List<Category>>> categories();

    @GET("api/v1/product/category/{id}/price")
    Call<ApiResponse<List<Product>>> productsByCategory(@Path("id") int categoryId);

    @GET("api/v1/user/{username}")
    Call<ApiResponse<User>> getUser(@Path("username") String username);
}
