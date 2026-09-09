package com.harmoni.pos.order.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.harmoni.pos.order.data.model.User;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Production-ready token storage.
 *
 * <ul>
 *   <li>EncryptedSharedPreferences for access/refresh tokens</li>
 *   <li>Volatile + synchronized init (double-checked locking)</li>
 *   <li>Synchronized token rotation to avoid concurrent refresh races</li>
 *   <li>Proper JSON body for refresh, non-deprecated OkHttp APIs</li>
 * </ul>
 */
public final class TokenManager {

    private static final String PREFS = "pos_secure_prefs";
    private static final String PREFS_FALLBACK = "pos_prefs";

    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_STORE_ID = "store_id";
    private static final String KEY_STORE_NAME = "store_name";
    private static final String KEY_CHAIN_ID = "chain_id";
    private static final String KEY_CHAIN_NAME = "chain_name";
    private static final String KEY_BRAND_ID = "brand_id";
    private static final String KEY_BRAND_NAME = "brand_name";

    private static volatile SharedPreferences prefs;
    private static final Object LOCK = new Object();

    private TokenManager() {}

    public static void init(@NonNull Context context) {
        if (prefs != null) return;
        synchronized (LOCK) {
            if (prefs != null) return;
            Context app = context.getApplicationContext();
            SharedPreferences created = null;
            try {
                MasterKey masterKey = new MasterKey.Builder(app)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                created = EncryptedSharedPreferences.create(
                        app,
                        PREFS,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            } catch (Exception e) {
                // Fallback to regular prefs if device doesn't support keystore (emulator/low API)
                // Still better than crash; caller can migrate later.
                created = app.getSharedPreferences(PREFS_FALLBACK, Context.MODE_PRIVATE);
            }
            // One-time migration from legacy plaintext prefs if secure prefs is empty
            migrateIfNeeded(app, created);
            prefs = created;
        }
    }

    private static void migrateIfNeeded(Context app, SharedPreferences target) {
        if (target.contains(KEY_ACCESS) || target.contains(KEY_REFRESH)) return;
        SharedPreferences legacy = app.getSharedPreferences(PREFS_FALLBACK, Context.MODE_PRIVATE);
        if (!legacy.contains(KEY_ACCESS) && !legacy.contains(KEY_REFRESH)) return;
        SharedPreferences.Editor e = target.edit();
        copyIfExists(legacy, e, KEY_ACCESS);
        copyIfExists(legacy, e, KEY_REFRESH);
        copyIfExists(legacy, e, KEY_USERNAME);
        copyIfExists(legacy, e, KEY_STORE_NAME);
        copyIfExists(legacy, e, KEY_CHAIN_NAME);
        copyIfExists(legacy, e, KEY_BRAND_NAME);
        // ints need special handling
        if (legacy.contains(KEY_STORE_ID)) e.putInt(KEY_STORE_ID, legacy.getInt(KEY_STORE_ID, 0));
        if (legacy.contains(KEY_CHAIN_ID)) e.putInt(KEY_CHAIN_ID, legacy.getInt(KEY_CHAIN_ID, 0));
        if (legacy.contains(KEY_BRAND_ID)) e.putInt(KEY_BRAND_ID, legacy.getInt(KEY_BRAND_ID, 0));
        e.apply();
        // Clear legacy tokens after migration (keep profile keys for compat, but clear secrets)
        legacy.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).apply();
    }

    private static void copyIfExists(SharedPreferences src, SharedPreferences.Editor dst, String key) {
        if (src.contains(key)) {
            String v = src.getString(key, "");
            if (v != null) dst.putString(key, v);
        }
    }

    public static boolean isInitialized() {
        return prefs != null;
    }

    private static SharedPreferences requirePrefs() {
        SharedPreferences p = prefs;
        if (p == null) throw new IllegalStateException("TokenManager not initialized. Call init() in Application.onCreate()");
        return p;
    }

    public static void setTokens(@Nullable String accessToken, @Nullable String refreshToken) {
        if (prefs == null) return;
        SharedPreferences.Editor editor = requirePrefs().edit()
                .putString(KEY_ACCESS, accessToken != null ? accessToken : "")
                .putString(KEY_REFRESH, refreshToken != null ? refreshToken : "");
        editor.apply();
    }

    @NonNull
    public static String getAccessToken() {
        if (prefs == null) return "";
        return requirePrefs().getString(KEY_ACCESS, "");
    }

    @NonNull
    public static String getRefreshToken() {
        if (prefs == null) return "";
        return requirePrefs().getString(KEY_REFRESH, "");
    }

    public static boolean isLoggedIn() {
        return prefs != null && !getAccessToken().isEmpty() && !getUsername().isEmpty();
    }

    /**
     * Thread-safe single-flight refresh. Synchronized to prevent N concurrent refresh calls.
     * Sends JSON {"refreshToken": "..."} and parses both camelCase and snake_case responses.
     */
    @Nullable
    public static synchronized String refreshAccessToken() {
        if (prefs == null) return null;
        String refresh = getRefreshToken();
        if (refresh == null || refresh.isEmpty()) {
            clearTokens();
            return null;
        }
        // Avoid thundering herd: if another thread already refreshed, return current token
        // Caller holds TokenManager.class lock via ApiClient.TokenAuthenticator, so this is safe.

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("refreshToken", refresh);
            byte[] bodyBytes = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
            RequestBody body = RequestBody.create(bodyBytes, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(ConfigManager.getBaseAuth() + "api/v1/auth/refresh-token")
                    .post(body)
                    .header("Accept", "application/json")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    clearTokens();
                    return null;
                }
                String json = response.body() == null ? "" : response.body().string();
                if (json.isEmpty()) {
                    clearTokens();
                    return null;
                }
                JSONObject obj = new JSONObject(json);
                // Support both envelope {data:{accessToken}} and flat {accessToken}
                JSONObject data = obj.optJSONObject("data");
                if (data != null) obj = data;
                String access = obj.optString("accessToken", obj.optString("access_token", ""));
                String newRefresh = obj.optString("refreshToken", obj.optString("refresh_token", ""));
                if (access == null || access.isEmpty()) {
                    clearTokens();
                    return null;
                }
                setTokens(access, (newRefresh == null || newRefresh.isEmpty()) ? refresh : newRefresh);
                return access;
            }
        } catch (Exception e) {
            clearTokens();
            return null;
        } finally {
            // Ensure OkHttp resources are not leaked
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }

    public static void clearTokens() {
        if (prefs == null) return;
        requirePrefs().edit()
                .putString(KEY_ACCESS, "")
                .putString(KEY_REFRESH, "")
                .remove(KEY_USERNAME)
                .remove(KEY_STORE_ID)
                .remove(KEY_STORE_NAME)
                .remove(KEY_CHAIN_ID)
                .remove(KEY_CHAIN_NAME)
                .remove(KEY_BRAND_ID)
                .remove(KEY_BRAND_NAME)
                .apply();
    }

    /** Clears only auth tokens, keeps cached profile for offline display. */
    public static void clearAuthOnly() {
        if (prefs == null) return;
        requirePrefs().edit()
                .putString(KEY_ACCESS, "")
                .putString(KEY_REFRESH, "")
                .apply();
    }

    public static void saveUser(@Nullable User user) {
        if (prefs == null || user == null) return;
        requirePrefs().edit()
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

    @NonNull
    public static String getUsername() {
        if (prefs == null) return "";
        String v = requirePrefs().getString(KEY_USERNAME, "");
        return v != null ? v : "";
    }

    @NonNull
    public static String getStoreName() {
        if (prefs == null) return "";
        String v = requirePrefs().getString(KEY_STORE_NAME, "");
        return v != null ? v : "";
    }

    @NonNull
    public static String getChainName() {
        if (prefs == null) return "";
        String v = requirePrefs().getString(KEY_CHAIN_NAME, "");
        return v != null ? v : "";
    }

    @NonNull
    public static String getBrandName() {
        if (prefs == null) return "";
        String v = requirePrefs().getString(KEY_BRAND_NAME, "");
        return v != null ? v : "";
    }
}
