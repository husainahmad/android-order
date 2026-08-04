package com.harmoni.pos.order.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("accessToken")
    private String accessToken;
    @SerializedName("refreshToken")
    private String refreshToken;

    public String getAccessToken() { return accessToken == null ? "" : accessToken; }
    public String getRefreshToken() { return refreshToken == null ? "" : refreshToken; }
}
