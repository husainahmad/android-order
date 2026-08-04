package com.harmoni.pos.order.data.remote;

import com.harmoni.pos.order.data.model.AuthResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {

    @POST("api/v1/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);
}
