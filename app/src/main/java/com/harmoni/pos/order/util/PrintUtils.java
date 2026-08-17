package com.harmoni.pos.order.util;

import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.OrderDetail;
import com.harmoni.pos.order.data.model.OrderDetailSku;
import com.harmoni.pos.order.data.model.Settlement;

import java.util.Locale;
import java.util.Map;

public class PrintUtils {

    private PrintUtils() {}

    public static String buildReceiptText(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("KOPI HARMONI\n");
        sb.append("----------------------------\n");
        sb.append("Order  : ").append(order.getOrderNo()).append("\n");
        sb.append("Date   : ").append(TimeUtils.toJakarta(order.getCreatedAt())).append("\n");
        sb.append("Cust   : ").append(order.getCustomerName()).append("\n");
        sb.append("Type   : ").append(order.getServiceTypeName()).append("\n");
        sb.append("----------------------------\n");
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                sb.append(detail.getProductName()).append("\n");
                if (detail.getOrderDetailSkus() != null) {
                    for (OrderDetailSku sku : detail.getOrderDetailSkus()) {
                        sb.append(String.format(Locale.US, "  %s  %.0f x %.0f = %.0f\n",
                                sku.getSkuName(), sku.getQuantity(), sku.getPrice(), sku.getAmount()));
                    }
                }
            }
        }
        sb.append("----------------------------\n");
        sb.append("Sub Total : ").append(CurrencyUtils.formatRp(order.getSubTotal())).append("\n");
        sb.append("Discount  : ").append(CurrencyUtils.formatRp(order.getDiscountTotal())).append("\n");
        sb.append("TOTAL     : ").append(CurrencyUtils.formatRp(order.getGrandTotal())).append("\n");
        sb.append("Payment   : ").append(order.getPaymentName()).append("\n");
        if (!order.getRemark().isEmpty()) {
            sb.append("Note      : ").append(order.getRemark()).append("\n");
        }
        sb.append("----------------------------\n");
        sb.append("Terima kasih\n");
        return sb.toString();
    }

    public static String buildKitchenText(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("KOPI HARMONI - KP\n");
        sb.append("----------------------------\n");
        sb.append("Order  : ").append(order.getOrderNo()).append("\n");
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                sb.append(detail.getProductName()).append("\n");
                if (detail.getOrderDetailSkus() != null) {
                    for (OrderDetailSku sku : detail.getOrderDetailSkus()) {
                        sb.append(String.format(Locale.US, "  %s x %d\n", sku.getSkuName(), (int) sku.getQuantity()));
                    }
                }
            }
        }
        sb.append("----------------------------\n");
        return sb.toString();
    }

    public static String buildSettlementText(Settlement settlement) {
        StringBuilder sb = new StringBuilder();
        sb.append("KOPI HARMONI\n");
        sb.append("SETTLEMENT\n");
        sb.append("----------------------------\n");
        sb.append("Total Orders   : ").append(settlement.getTotalOrders()).append("\n");
        sb.append("Total Sales    : ").append(CurrencyUtils.formatRp2(settlement.getTotalSales())).append("\n");
        sb.append("Total Discount : ").append(CurrencyUtils.formatRp2(settlement.getTotalDiscounts())).append("\n");
        sb.append("Total Tax      : ").append(CurrencyUtils.formatRp2(settlement.getTotalTax())).append("\n");
        sb.append("Net Sales      : ").append(CurrencyUtils.formatRp2(settlement.getTotalNetSales())).append("\n");
        if (settlement.getPaymentBreakdown() != null) {
            for (Map.Entry<String, Double> entry : settlement.getPaymentBreakdown().entrySet()) {
                sb.append(entry.getKey()).append(" : ")
                        .append(CurrencyUtils.formatRp2(entry.getValue())).append("\n");
            }
        }
        sb.append("----------------------------\n");
        return sb.toString();
    }
}
