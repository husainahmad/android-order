package com.harmoni.pos.order.data.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("data")
    private T data;
    private String message;
    private int code;

    public T getData() { return data; }
    public String getMessage() { return message == null ? "" : message; }
    public int getCode() { return code; }
}
