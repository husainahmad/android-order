package com.harmoni.pos.order.ui.reports;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.SalesReportRow;
import com.harmoni.pos.order.data.remote.ApiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SalesReportViewModel extends ViewModel {

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<SalesReportRow>> rows = new MutableLiveData<>();

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<List<SalesReportRow>> getRows() {
        return rows;
    }

    public void load(String startDate, String endDate) {
        loading.setValue(true);
        String start = startDate + "T00:00:00";
        String end = endDate + "T23:59:59";
        ApiClient.orderService().salesReport(start, end).enqueue(new Callback<ApiResponse<List<SalesReportRow>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SalesReportRow>>> call, Response<ApiResponse<List<SalesReportRow>>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    rows.setValue(response.body().getData());
                    error.setValue("");
                } else {
                    error.setValue("Failed to load report: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SalesReportRow>>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }
}
