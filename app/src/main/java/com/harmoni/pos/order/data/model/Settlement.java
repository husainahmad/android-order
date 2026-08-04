package com.harmoni.pos.order.data.model;

import java.util.Map;

public class Settlement {
    private int totalOrders;
    private double totalSales;
    private double totalDiscounts;
    private double totalTax;
    private double totalNetSales;
    private Map<String, Double> paymentBreakdown;

    public int getTotalOrders() { return totalOrders; }
    public double getTotalSales() { return totalSales; }
    public double getTotalDiscounts() { return totalDiscounts; }
    public double getTotalTax() { return totalTax; }
    public double getTotalNetSales() { return totalNetSales; }
    public Map<String, Double> getPaymentBreakdown() { return paymentBreakdown; }
}
