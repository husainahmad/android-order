package com.harmoni.pos.order.data.model;

import java.util.List;

public class DailyReportRow {
    private String date;
    private List<ProductRow> products;

    public String getDate() { return date == null ? "" : date; }
    public List<ProductRow> getProducts() { return products; }

    public static class ProductRow {
        private String productName;
        private int quantity;
        private double netSales;
        private double discount;

        public String getProductName() { return productName == null ? "" : productName; }
        public int getQuantity() { return quantity; }
        public double getNetSales() { return netSales; }
        public double getDiscount() { return discount; }
    }
}
