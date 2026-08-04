package com.harmoni.pos.order.ui.login;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.harmoni.pos.order.PosApplication;
import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.AuthResponse;
import com.harmoni.pos.order.data.model.User;
import com.harmoni.pos.order.data.remote.ApiClient;
import com.harmoni.pos.order.data.remote.LoginRequest;
import com.harmoni.pos.order.data.remote.TokenManager;
import com.harmoni.pos.order.util.JsonCache;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loggedIn = new MutableLiveData<>(false);

    public LoginViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoggedIn() {
        return loggedIn;
    }

    public void login(String username, String password) {
        if (username.trim().isEmpty() || password.isEmpty()) {
            error.setValue("Please enter username and password");
            return;
        }
        loading.setValue(true);
        ApiClient.authService().login(new LoginRequest(username.trim(), password))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AuthResponse> call,
                                           @NonNull Response<AuthResponse> response) {
                        loading.setValue(false);
                        if (response.isSuccessful() && response.body() != null
                                && !response.body().getAccessToken().isEmpty()) {
                            AuthResponse auth = response.body();
                            TokenManager.setTokens(auth.getAccessToken(), auth.getRefreshToken());
                            fetchUser(username.trim());
                        } else {
                            error.setValue("Login failed: HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AuthResponse> call,
                                          @NonNull Throwable t) {
                        loading.setValue(false);
                        error.setValue("Network error: " + t.getMessage());
                    }
                });
    }

    private void fetchUser(String username) {
        ApiClient.menuService().getUser(username).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                   @NonNull Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    TokenManager.saveUser(response.body().getData());
                    JsonCache.clear(PosApplication.getAppContext());
                    loggedIn.setValue(true);
                } else {
                    error.setValue("Failed to load user: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }
}
