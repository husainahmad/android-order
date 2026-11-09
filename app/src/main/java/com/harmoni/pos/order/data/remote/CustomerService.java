package com.harmoni.pos.order.data.remote;

import com.harmoni.pos.order.data.model.CreateCustomerRequest;
import com.harmoni.pos.order.data.model.Customer;
import com.harmoni.pos.order.data.model.CustomerPage;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Customer service endpoints. Unlike the menu/order services these answer with the
 * resource directly rather than the {@code { data: ... }} envelope.
 */
public interface CustomerService {

    @GET("api/v1/customers")
    Call<CustomerPage> searchCustomers(@Query("search") String search,
                                       @Query("page") int page,
                                       @Query("size") int size);

    @POST("api/v1/customers")
    Call<Customer> createCustomer(@Body CreateCustomerRequest request);
}
