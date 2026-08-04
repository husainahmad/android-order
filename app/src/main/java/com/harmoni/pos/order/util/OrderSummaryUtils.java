package com.harmoni.pos.order.util;

import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.OrderDetail;
import com.harmoni.pos.order.data.model.OrderDetailSku;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderSummaryUtils {

    public static class Summary {
        public int totalOrders;
        public Map<String, Double> paymentTotals = new LinkedHashMap<>();
        public List<Map.Entry<String, Integer>> topItems = new ArrayList<>();
        public double totalSales;
    }

    private OrderSummaryUtils() {}

    public static Summary compute(List<Order> orders) {
        Summary s = new Summary();
        if (orders == null) return s;
        s.totalOrders = orders.size();
        Map<String, Integer> itemQty = new HashMap<>();
        for (Order o : orders) {
            String payment = o.getPaymentName();
            s.paymentTotals.merge(payment, o.getGrandTotal(), Double::sum);
            s.totalSales += o.getGrandTotal();
            if (o.getOrderDetails() != null) {
                for (OrderDetail d : o.getOrderDetails()) {
                    if (d.getOrderDetailSkus() != null) {
                        for (OrderDetailSku sku : d.getOrderDetailSkus()) {
                            itemQty.merge(d.getProductName(), (int) sku.getQuantity(), Integer::sum);
                        }
                    }
                }
            }
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(itemQty.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        s.topItems = sorted.size() > 50 ? new ArrayList<>(sorted.subList(0, 50)) : sorted;
        return s;
    }
}
