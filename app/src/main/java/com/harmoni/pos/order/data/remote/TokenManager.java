package com.harmoni.pos.order.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import com.harmoni.pos.order.data.model.User;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TokenManager {

    private static final String PREFS = "pos_prefs";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_STORE_ID = "store_id";
    private static final String KEY_STORE_NAME = "store_name";
    private static final String KEY_CHAIN_ID = "chain_id";
    private static final String KEY_CHAIN_NAME = "chain_name";
    private static final String KEY_BRAND_ID = "brand_id";
    private static final String KEY_BRAND_NAME = "brand_name";

    private static SharedPreferences prefs;

    private TokenManager() {}

    public static void init(Context context) {
        if (prefs == null) {
            prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
    }

    public static boolean isInitialized() {
        return prefs != null;
    }

    public static void setTokens(String accessToken, String refreshToken) {
        if (prefs == null) return;
        prefs.edit()
                .putString(KEY_ACCESS, accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .apply();
    }

    public static String getAccessToken() {
        if (prefs == null) return "";
        return prefs.getString(KEY_ACCESS, "");
    }

    public static String getRefreshToken() {
        if (prefs == null) return "";
        return prefs.getString(KEY_REFRESH, "");
    }

    public static boolean isLoggedIn() {
        return prefs != null && !getAccessToken().isEmpty() && !getUsername().isEmpty();
    }

    public static synchronized String refreshAccessToken() {
        if (prefs == null) return null;
        String refresh = getRefreshToken();
        if (refresh.isEmpty()) {
            clearTokens();
            return null;
        }
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), refresh);
            Request request = new Request.Builder()
                    .url(ConfigManager.getBaseAuth() + "api/v1/auth/refresh-token")
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    clearTokens();
                    return null;
                }
                String json = response.body() == null ? "" : response.body().string();
                JSONObject obj = new JSONObject(json);
                String access = obj.optString("accessToken", "");
                String newRefresh = obj.optString("refreshToken", "");
                if (access.isEmpty()) {
                    clearTokens();
                    return null;
                }
                setTokens(access, newRefresh.isEmpty() ? refresh : newRefresh);
                return access;
            }
        } catch (Exception e) {
            clearTokens();
            return null;
        }
    }

    public static void clearTokens() {
        if (prefs == null) return;
        prefs.edit()
                .putString(KEY_ACCESS, "")
                .putString(KEY_REFRESH, "")
                .apply();
    }

    public static void saveUser(User user) {
        if (prefs == null || user == null) return;
        prefs.edit()
                .putString(KEY_USERNAME, user.getUsername())
                .putInt(KEY_STORE_ID, user.getStore() != null ? user.getStore().getId() : 0)
                .putString(KEY_STORE_NAME, user.getStoreName())
                .putInt(KEY_CHAIN_ID, user.getStore() != null && user.getStore().getChain() != null
                        ? user.getStore().getChain().getId() : 0)
                .putString(KEY_CHAIN_NAME, user.getChainName())
                .putInt(KEY_BRAND_ID, user.getStore() != null && user.getStore().getChain() != null
                        && user.getStore().getChain().getBrand() != null
                        ? user.getStore().getChain().getBrand().getId() : 0)
                .putString(KEY_BRAND_NAME, user.getBrandName())
                .apply();
    }

    public static String getUsername() {
        if (prefs == null) return "";
        return prefs.getString(KEY_USERNAME, "");
    }

    public static String getStoreName() {
        if (prefs == null) return "";
        return prefs.getString(KEY_STORE_NAME, "");
    }

    public static String getChainName() {
        if (prefs == null) return "";
        return prefs.getString(KEY_CHAIN_NAME, "");
    }

    public static String getBrandName() {
        if (prefs == null) return "";
        return prefs.getString(KEY_BRAND_NAME, "");
    }
}
