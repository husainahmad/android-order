package com.harmoni.pos.order.data.remote;

import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.DailyReportRow;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.OrderRequest;
import com.harmoni.pos.order.data.model.OrderVolume;
import com.harmoni.pos.order.data.model.PaymentRequest;
import com.harmoni.pos.order.data.model.SalesReportRow;
import com.harmoni.pos.order.data.model.Settlement;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OrderService {

    @GET("api/v1/order")
    Call<ApiResponse<List<Order>>> dailyOrders();

    @POST("api/v1/order")
    Call<ApiResponse<Order>> confirmOrder(@Body OrderRequest request);

    @PUT("api/v1/order")
    Call<ApiResponse<Order>> payOrder(@Body PaymentRequest request);

    @PUT("api/v1/order/{id}/void")
    Call<ApiResponse<Object>> voidOrder(@Path("id") int orderId);

    @GET("api/v1/reports/settlement")
    Call<ApiResponse<Settlement>> settlement(@Query("start") String start, @Query("end") String end);

    @GET("api/v1/reports/sales")
    Call<ApiResponse<List<SalesReportRow>>> salesReport(@Query("start") String start, @Query("end") String end);

    @GET("api/v1/reports/daily")
    Call<ApiResponse<List<DailyReportRow>>> dailyReport(@Query("start") String start, @Query("end") String end);

    @GET("api/v1/reports/order-volume")
    Call<ApiResponse<OrderVolume>> orderVolume(@Query("start") String start, @Query("end") String end);
}
