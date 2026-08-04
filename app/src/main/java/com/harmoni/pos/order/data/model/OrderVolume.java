package com.harmoni.pos.order.data.model;

public class OrderVolume {
    private int totalOrders;
    private int peakTimeOrders;
    private int nonPeakTimeOrders;

    public int getTotalOrders() { return totalOrders; }
    public int getPeakTimeOrders() { return peakTimeOrders; }
    public int getNonPeakTimeOrders() { return nonPeakTimeOrders; }
}
