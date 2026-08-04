package com.harmoni.pos.order.data.remote;

import androidx.annotation.NonNull;

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

public class ApiClient {

    private static OkHttpClient httpClient;
    private static Retrofit authRetrofit;
    private static Retrofit menuRetrofit;
    private static Retrofit orderRetrofit;

    private ApiClient() {}

    private static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor())
                    .authenticator(new TokenAuthenticator())
                    .addInterceptor(logging)
                    .build();
        }
        return httpClient;
    }

    private static Retrofit getAuthRetrofit() {
        if (authRetrofit == null) {
            authRetrofit = buildRetrofit(ConfigManager.getBaseAuth());
        }
        return authRetrofit;
    }

    private static Retrofit getMenuRetrofit() {
        if (menuRetrofit == null) {
            menuRetrofit = buildRetrofit(ConfigManager.getBaseMenu());
        }
        return menuRetrofit;
    }

    private static Retrofit getOrderRetrofit() {
        if (orderRetrofit == null) {
            orderRetrofit = buildRetrofit(ConfigManager.getBaseOrder());
        }
        return orderRetrofit;
    }

    public static synchronized void reset() {
        authRetrofit = null;
        menuRetrofit = null;
        orderRetrofit = null;
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

    private static class AuthInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request original = chain.request();
            String token = TokenManager.getAccessToken();
            if (!token.isEmpty()) {
                Request.Builder builder = original.newBuilder()
                        .header("Authorization", "Bearer " + token);
                return chain.proceed(builder.build());
            }
            return chain.proceed(original);
        }
    }

    private static class TokenAuthenticator implements Authenticator {
        @Override
        public Request authenticate(Route route, Response response) {
            synchronized (TokenManager.class) {
                String newToken = TokenManager.refreshAccessToken();
                if (newToken == null || newToken.isEmpty()) {
                    return null;
                }
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + newToken)
                        .build();
            }
        }
    }
}
