package com.harmoni.pos.order.ui.reports;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.DailyReportRow;
import com.harmoni.pos.order.data.model.OrderVolume;
import com.harmoni.pos.order.data.remote.ApiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DailyReportViewModel extends ViewModel {

    public static class Row {
        public String date;
        public String productName;
        public int quantity;
        public double netSales;
        public double discount;

        public Row(String date, String productName, int quantity, double netSales, double discount) {
            this.date = date;
            this.productName = productName;
            this.quantity = quantity;
            this.netSales = netSales;
            this.discount = discount;
        }
    }

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<Row>> rows = new MutableLiveData<>();
    private final MutableLiveData<OrderVolume> volume = new MutableLiveData<>();

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<List<Row>> getRows() {
        return rows;
    }

    public LiveData<OrderVolume> getVolume() {
        return volume;
    }

    public void load(String startDate, String endDate) {
        loading.setValue(true);
        String start = startDate + "T00:00:00";
        String end = endDate + "T23:59:59";

        ApiClient.orderService().dailyReport(start, end)
                .enqueue(new Callback<ApiResponse<List<DailyReportRow>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<DailyReportRow>>> call,
                                           Response<ApiResponse<List<DailyReportRow>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            rows.setValue(flatten(response.body().getData()));
                            error.setValue("");
                        } else {
                            error.setValue("Failed to load report: HTTP " + response.code());
                        }
                        loading.setValue(false);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<DailyReportRow>>> call, Throwable t) {
                        loading.setValue(false);
                        error.setValue("Network error: " + t.getMessage());
                    }
                });

        ApiClient.orderService().orderVolume(start, end)
                .enqueue(new Callback<ApiResponse<OrderVolume>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<OrderVolume>> call,
                                           Response<ApiResponse<OrderVolume>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            volume.setValue(response.body().getData());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<OrderVolume>> call, Throwable t) {
                    }
                });
    }

    private static List<Row> flatten(List<DailyReportRow> data) {
        List<Row> result = new java.util.ArrayList<>();
        if (data == null) return result;
        for (DailyReportRow r : data) {
            if (r.getProducts() != null) {
                for (DailyReportRow.ProductRow p : r.getProducts()) {
                    result.add(new Row(r.getDate(), p.getProductName(), p.getQuantity(),
                            p.getNetSales(), p.getDiscount()));
                }
            }
        }
        return result;
    }
}
