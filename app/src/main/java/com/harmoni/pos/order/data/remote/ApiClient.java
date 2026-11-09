package com.harmoni.pos.order.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.harmoni.pos.order.BuildConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static volatile OkHttpClient httpClient;
    private static volatile Retrofit authRetrofit;
    private static volatile Retrofit menuRetrofit;
    private static volatile Retrofit orderRetrofit;
    private static volatile Retrofit customerRetrofit;
    private static final Object LOCK = new Object();

    private ApiClient() {}

    private static OkHttpClient getHttpClient() {
        OkHttpClient c = httpClient;
        if (c != null) return c;
        synchronized (LOCK) {
            c = httpClient;
            if (c != null) return c;

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // Never log Authorization header in release (PII)
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BASIC
                    : HttpLoggingInterceptor.Level.NONE);
            // Redact Authorization header even in debug logs
            logging.redactHeader("Authorization");

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor())
                    .addInterceptor(new SessionExpiredInterceptor())
                    .authenticator(new TokenAuthenticator());

            // Only add logging in DEBUG to avoid PII leak via logcat in release
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(logging);
            }

            httpClient = builder.build();
            return httpClient;
        }
    }

    private static Retrofit getAuthRetrofit() {
        Retrofit r = authRetrofit;
        if (r != null) return r;
        synchronized (LOCK) {
            r = authRetrofit;
            if (r != null) return r;
            r = buildRetrofit(ConfigManager.getBaseAuth());
            authRetrofit = r;
            return r;
        }
    }

    private static Retrofit getMenuRetrofit() {
        Retrofit r = menuRetrofit;
        if (r != null) return r;
        synchronized (LOCK) {
            r = menuRetrofit;
            if (r != null) return r;
            r = buildRetrofit(ConfigManager.getBaseMenu());
            menuRetrofit = r;
            return r;
        }
    }

    private static Retrofit getOrderRetrofit() {
        Retrofit r = orderRetrofit;
        if (r != null) return r;
        synchronized (LOCK) {
            r = orderRetrofit;
            if (r != null) return r;
            r = buildRetrofit(ConfigManager.getBaseOrder());
            orderRetrofit = r;
            return r;
        }
    }

    private static Retrofit getCustomerRetrofit() {
        Retrofit r = customerRetrofit;
        if (r != null) return r;
        synchronized (LOCK) {
            r = customerRetrofit;
            if (r != null) return r;
            r = buildRetrofit(ConfigManager.getBaseCustomer());
            customerRetrofit = r;
            return r;
        }
    }

    /**
     * Clears cached clients after server config change or logout.
     * Also shuts down previous OkHttp resources to avoid leaks.
     */
    public static void reset() {
        synchronized (LOCK) {
            if (httpClient != null) {
                httpClient.dispatcher().executorService().shutdown();
                httpClient.connectionPool().evictAll();
            }
            httpClient = null;
            authRetrofit = null;
            menuRetrofit = null;
            orderRetrofit = null;
            customerRetrofit = null;
        }
    }

    private static Retrofit buildRetrofit(String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(getHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static AuthService authService() {
        return getAuthRetrofit().create(AuthService.class);
    }

    public static MenuService menuService() {
        return getMenuRetrofit().create(MenuService.class);
    }

    public static OrderService orderService() {
        return getOrderRetrofit().create(OrderService.class);
    }

    public static CustomerService customerService() {
        return getCustomerRetrofit().create(CustomerService.class);
    }

    private static class AuthInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request original = chain.request();
            String path = original.url().encodedPath();
            // Do not attach token to auth endpoints (login/refresh)
            if (path.startsWith("/api/v1/auth/")) {
                return chain.proceed(original);
            }
            String token = TokenManager.getAccessToken();
            if (token != null && !token.isEmpty()) {
                Request authorised = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(authorised);
            }
            return chain.proceed(original);
        }
    }

    /**
     * Centralized 403 (and final 401) handler.
     * Fires exactly once per burst via SessionManager throttle.
     * Runs as application interceptor so response.code() is final status after Authenticator retries.
     */
    private static class SessionExpiredInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            int code = response.code();
            // 403 = Forbidden (RBAC / revoked token). 401 here means refresh already failed.
            if (code == 403 || code == 401) {
                String path = response.request().url().encodedPath();
                // Don't trigger logout for login/refresh itself – let ViewModel show error
                boolean isAuthEndpoint = path.startsWith("/api/v1/auth/");
                if (!isAuthEndpoint) {
                    // Clear credentials so next launch starts clean; keep UI cache
                    try {
                        TokenManager.clearTokens();
                    } catch (Exception ignored) {}
                    // Reset cached OkHttp/Retrofit so next request picks up cleared state
                    // Don't call ApiClient.reset() here (deadlock with getHttpClient LOCK) –
                    // just invalidate via lazy recreation on next call; SessionManager will trigger UI.
                    if (code == 403) {
                        SessionManager.notifySessionExpired("Access denied (403)");
                    } else {
                        SessionManager.notifySessionExpired("Session expired (401)");
                    }
                }
            }
            return response;
        }
    }

    private static class TokenAuthenticator implements Authenticator {
        @Nullable
        @Override
        public Request authenticate(@Nullable Route route, @NonNull Response response) {
            // Avoid infinite loop: only try once (OkHttp will retry up to 3x otherwise)
            if (responseCount(response) >= 2) {
                return null;
            }
            // Only handle 401s - do not retry on 403/500 etc.
            if (response.code() != 401) {
                return null;
            }

            synchronized (TokenManager.class) {
                // If request already had a newer token than current storage, don't refresh again
                String currentToken = TokenManager.getAccessToken();
                String requestToken = extractToken(response.request());
                if (currentToken != null && !currentToken.isEmpty()
                        && !currentToken.equals(requestToken)) {
                    // Another thread already refreshed - retry with current token
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + currentToken)
                            .build();
                }

                String newToken = TokenManager.refreshAccessToken();
                if (newToken == null || newToken.isEmpty()) {
                    return null;
                }
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + newToken)
                        .build();
            }
        }

        private int responseCount(Response response) {
            int count = 1;
            Response prior = response.priorResponse();
            while (prior != null) {
                count++;
                prior = prior.priorResponse();
            }
            return count;
        }

        @Nullable
        private String extractToken(Request request) {
            String h = request.header("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                return h.substring(7);
            }
            return null;
        }
    }
}
