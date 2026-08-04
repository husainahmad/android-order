package com.harmoni.pos.order.data.model;

public class SalesReportRow {
    private String categoryName;
    private String productName;
    private int quantity;
    private double grossSales;
    private double discount;
    private double netSales;

    public String getCategoryName() { return categoryName == null ? "" : categoryName; }
    public String getProductName() { return productName == null ? "" : productName; }
    public int getQuantity() { return quantity; }
    public double getGrossSales() { return grossSales; }
    public double getDiscount() { return discount; }
    public double getNetSales() { return netSales; }
}
