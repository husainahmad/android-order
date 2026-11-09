package com.harmoni.pos.order.util;

import android.content.Context;

import com.harmoni.pos.order.PosApplication;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.OrderCustomizationLine;
import com.harmoni.pos.order.data.model.OrderDetail;
import com.harmoni.pos.order.data.model.OrderDetailSku;
import com.harmoni.pos.order.data.model.Settlement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PrintUtils {

    private PrintUtils() {}

    private static final int WIDTH = 32;
    private static final int AMOUNT_WIDTH = 10;
    private static final int ITEM_GAP = 1;

    public static String buildReceiptText(Order order) {
        if (order == null) return "";

        StringBuilder sb = new StringBuilder();

        sb.append(center("KOPI HARMONI")).append('\n');
        sb.append(divider()).append('\n');

        appendInfo(sb, "Order", order.getOrderNo());
        appendInfo(sb, "Date", TimeUtils.toJakarta(order.getCreatedAt()));
        appendInfo(sb, "Cust", order.getCustomerName());
        appendInfo(sb, "Type", order.getServiceTypeName());

        sb.append(divider()).append('\n');

        if (order.getOrderDetails() != null) {
            boolean first = true;
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail == null) continue;

                if (!first) sb.append('\n');
                first = false;

                appendWrappedLine(sb, detail.getProductName());

                for (OrderDetailSku sku : detail.getOrderDetailSkus()) {
                    if (sku == null) continue;
                    appendSku(sb, sku);
                    for (OrderCustomizationLine line : detail.getCustomizationsForSku(sku)) {
                        appendCustomizationLine(sb, line, true);
                    }
                }
                // Older orders stored the choices against the product line only.
                for (OrderCustomizationLine line : detail.getUnattachedCustomizations()) {
                    appendCustomizationLine(sb, line, true);
                }
            }
        }

        sb.append(divider()).append('\n');

        sb.append(labelAmount("Sub Total", currency(order.getSubTotal()))).append('\n');

        long discount = Math.round(order.getDiscountTotal());
        if (discount > 0) {
            sb.append(labelAmount("Discount", "-" + currency(discount))).append('\n');
        }

        sb.append(doubleDivider()).append('\n');
        sb.append(labelAmount("TOTAL", currency(order.getGrandTotal()))).append('\n');
        sb.append(doubleDivider()).append('\n');

        appendInfo(sb, "Payment", order.getPaymentName());

        String remark = safe(order.getRemark());
        if (!remark.isEmpty()) {
            appendWrappedInfo(sb, "Note", remark);
        }

        sb.append(divider()).append('\n');
        String[] footer = footerText();
        sb.append(center(footer[0])).append('\n');
        sb.append(center(footer[1])).append('\n');

        return sb.toString();
    }

    /**
     * One kitchen ticket per category, each with the customer, service type, note and
     * the customization choices (without prices, which the kitchen does not need).
     */
    public static List<String> buildKitchenTexts(Order order) {
        List<String> tickets = new ArrayList<>();
        if (order == null || order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return tickets;
        }
        Map<Integer, List<OrderDetail>> grouped = new LinkedHashMap<>();
        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail == null) continue;
            grouped.computeIfAbsent(detail.getCategoryId(), k -> new ArrayList<>()).add(detail);
        }
        for (Map.Entry<Integer, List<OrderDetail>> entry : grouped.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(center("KOPI HARMONI - KP")).append('\n');
            sb.append(divider()).append('\n');
            appendInfo(sb, "Order", order.getOrderNo());
            appendInfo(sb, "Type", order.getServiceTypeName());
            appendInfo(sb, "Cust", order.getCustomerName());
            String remark = safe(order.getRemark());
            if (!remark.isEmpty()) appendWrappedInfo(sb, "Note", remark);
            sb.append(divider()).append('\n');

            String categoryName = entry.getValue().isEmpty()
                    ? "" : entry.getValue().get(0).getCategoryName();
            if (categoryName.isEmpty()) categoryName = "CATEGORY " + entry.getKey();
            sb.append(center("== " + categoryName + " ==")).append('\n');

            for (OrderDetail detail : entry.getValue()) {
                appendWrappedLine(sb, detail.getProductName());
                String detailNote = safe(detail.getRemark());
                if (!detailNote.isEmpty()) appendWrappedInfo(sb, "  Note", detailNote);
                for (OrderDetailSku sku : detail.getOrderDetailSkus()) {
                    if (sku == null) continue;
                    appendWrappedLine(sb, "  " + safe(sku.getSkuName())
                            + " x " + quantity(sku.getQuantity()));
                    for (OrderCustomizationLine line : detail.getCustomizationsForSku(sku)) {
                        appendCustomizationLine(sb, line, false);
                    }
                }
                for (OrderCustomizationLine line : detail.getUnattachedCustomizations()) {
                    appendCustomizationLine(sb, line, false);
                }
            }
            sb.append(divider()).append('\n');
            tickets.add(sb.toString());
        }
        return tickets;
    }

    /** Combined kitchen text for preview (all category tickets joined). */
    public static String buildKitchenText(Order order) {
        List<String> tickets = buildKitchenTexts(order);
        if (!tickets.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String ticket : tickets) sb.append(ticket);
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(center("KOPI HARMONI - KP")).append('\n');
        sb.append(divider()).append('\n');
        if (order != null) {
            appendInfo(sb, "Order", order.getOrderNo());
            appendInfo(sb, "Type", order.getServiceTypeName());
        }
        sb.append(divider()).append('\n');
        return sb.toString();
    }

    public static String buildSettlementText(Settlement settlement) {
        if (settlement == null) return "";

        StringBuilder sb = new StringBuilder();

        sb.append(center("KOPI HARMONI")).append('\n');
        sb.append(center("SETTLEMENT")).append('\n');
        sb.append(divider()).append('\n');

        sb.append(labelAmount("Total Orders", String.valueOf(settlement.getTotalOrders()))).append('\n');
        sb.append(labelAmount("Total Sales", CurrencyUtils.formatRp2(settlement.getTotalSales()))).append('\n');
        sb.append(labelAmount("Total Discount", CurrencyUtils.formatRp2(settlement.getTotalDiscounts()))).append('\n');
        sb.append(labelAmount("Total Tax", CurrencyUtils.formatRp2(settlement.getTotalTax()))).append('\n');
        sb.append(labelAmount("Net Sales", CurrencyUtils.formatRp2(settlement.getTotalNetSales()))).append('\n');

        if (settlement.getPaymentBreakdown() != null && !settlement.getPaymentBreakdown().isEmpty()) {
            sb.append(divider()).append('\n');
            for (Map.Entry<String, Double> entry : settlement.getPaymentBreakdown().entrySet()) {
                sb.append(labelAmount(entry.getKey(), CurrencyUtils.formatRp2(entry.getValue()))).append('\n');
            }
        }

        sb.append(divider()).append('\n');
        return sb.toString();
    }

    private static void appendSku(StringBuilder sb, OrderDetailSku sku) {
        String skuName = safe(sku.getSkuName());
        String qty = quantity(sku.getQuantity());
        String price = unitPrice(sku.getPrice());
        String amount = plainCurrency(sku.getAmount());

        int descriptionWidth = WIDTH - AMOUNT_WIDTH - ITEM_GAP;

        String detail = "  " + skuName + "  " + qty + "x" + price;

        if (detail.length() <= descriptionWidth) {
            sb.append(padRight(detail, descriptionWidth));
            sb.append(spaces(ITEM_GAP));
            sb.append(padLeft(amount, AMOUNT_WIDTH));
            sb.append('\n');
            return;
        }

        String prefix = "  ";
        int availableSkuWidth = descriptionWidth - prefix.length();
        if (availableSkuWidth <= 0) return;

        if (skuName.length() > availableSkuWidth) {
            for (int start = 0; start < skuName.length(); start += availableSkuWidth) {
                int end = Math.min(start + availableSkuWidth, skuName.length());
                sb.append(prefix).append(skuName.substring(start, end)).append('\n');
            }
        } else {
            sb.append(prefix).append(skuName).append('\n');
        }

        String quantityPrice = "  " + qty + "x" + price;
        sb.append(padRight(quantityPrice, descriptionWidth));
        sb.append(spaces(ITEM_GAP));
        sb.append(padLeft(amount, AMOUNT_WIDTH));
        sb.append('\n');
    }

    /** Group header, e.g. "    * Sugar", then the option line under it. */
    private static void appendCustomizationLine(StringBuilder sb, OrderCustomizationLine line,
                                                boolean showPrice) {
        String indent = "    - ";
        String text = indent + line.getGroupedDisplayName();
        if (showPrice && line.getAmount() > 0) {
            String right = "+" + plainCurrency(line.getAmount());
            int leftWidth = WIDTH - right.length() - 1;
            if (text.length() <= leftWidth) {
                sb.append(padRight(text, leftWidth)).append(' ').append(right).append('\n');
                return;
            }
        }
        appendWrappedLine(sb, text);
    }

    private static void appendInfo(StringBuilder sb, String label, String value) {
        String line = String.format(Locale.US, "%-6s: %s", label, safe(value));
        appendWrappedLine(sb, line);
    }

    private static void appendWrappedInfo(StringBuilder sb, String label, String value) {
        String prefix = String.format(Locale.US, "%-6s: ", label);
        String remaining = safe(value);
        int available = WIDTH - prefix.length();

        if (remaining.length() <= available) {
            sb.append(prefix).append(remaining).append('\n');
            return;
        }

        sb.append(prefix).append(remaining.substring(0, available)).append('\n');

        int start = available;
        while (start < remaining.length()) {
            int end = Math.min(start + WIDTH, remaining.length());
            sb.append(spaces(prefix.length()));
            sb.append(remaining.substring(start, end)).append('\n');
            start = end;
        }
    }

    private static void appendWrappedLine(StringBuilder sb, String value) {
        value = safe(value);
        if (value.length() <= WIDTH) {
            sb.append(value).append('\n');
            return;
        }

        for (int start = 0; start < value.length(); start += WIDTH) {
            int end = Math.min(start + WIDTH, value.length());
            sb.append(value.substring(start, end)).append('\n');
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String spaces(int count) {
        return count <= 0 ? "" : " ".repeat(count);
    }

    private static String repeat(char c, int count) {
        return count <= 0 ? "" : String.valueOf(c).repeat(count);
    }

    private static String divider() {
        return repeat('-', WIDTH);
    }

    private static String doubleDivider() {
        return repeat('=', WIDTH);
    }

    private static String center(String value) {
        value = safe(value);
        if (value.length() >= WIDTH) return value.substring(0, WIDTH);

        int totalPadding = WIDTH - value.length();
        int left = totalPadding / 2;
        int right = totalPadding - left;

        return spaces(left) + value + spaces(right);
    }

    private static String padRight(String value, int width) {
        value = safe(value);
        if (value.length() >= width) return value.substring(0, width);
        return value + spaces(width - value.length());
    }

    private static String padLeft(String value, int width) {
        value = safe(value);
        if (value.length() >= width) return value.substring(value.length() - width);
        return spaces(width - value.length()) + value;
    }

    private static String currency(long value) {
        return CurrencyUtils.formatRp(value);
    }

    private static String currency(double value) {
        return CurrencyUtils.formatRp(Math.round(value));
    }

    private static String plainCurrency(long value) {
        return CurrencyUtils.formatPlain(value);
    }

    private static String plainCurrency(double value) {
        return CurrencyUtils.formatPlain(value);
    }

    private static String unitPrice(double value) {
        return plainCurrency(value);
    }

    private static String quantity(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format(Locale.US, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private static String labelAmount(String label, String amount) {
        int labelWidth = WIDTH - AMOUNT_WIDTH;
        return padRight(label, labelWidth) + padLeft(amount, AMOUNT_WIDTH);
    }

    private static String[] footerText() {
        Context context = null;
        try {
            context = PosApplication.getAppContext();
        } catch (Exception ignored) {
            // Not running under the application (e.g. a unit test): fall back to the default.
        }
        return ReceiptLanguage.footer(context);
    }
}
