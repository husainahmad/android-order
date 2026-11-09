package com.harmoni.pos.order.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.OrderDetail;
import com.harmoni.pos.order.data.model.OrderDetailCustomization;
import com.harmoni.pos.order.data.model.OrderDetailSku;
import com.harmoni.pos.order.data.model.OrderCustomizationOption;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PrintUtilsTest {

    @Test
    public void receiptIncludesCustomizationChoices() {
        Order order = order();
        String receipt = PrintUtils.buildReceiptText(order);

        assertTrue(receipt.contains("Kopi"));
        assertTrue(receipt.contains("Sugar: Less Sugar"));
    }

    @Test
    public void kitchenPrintSplitsOneTicketPerCategory() {
        Order order = order();

        List<String> tickets = PrintUtils.buildKitchenTexts(order);

        assertEquals(2, tickets.size());
        assertTrue(tickets.get(0).contains("Drinks"));
        assertTrue(tickets.get(1).contains("Food"));
        assertTrue(tickets.get(0).contains("Less Sugar"));
    }

    // --- builders -----------------------------------------------------------

    private static Order order() {
        OrderDetailCustomization sugar = customization(100, "Sugar", "Less Sugar");
        OrderDetailSku sku = sku(100, "Regular", 1, 17000, 17000);
        OrderDetail drinks = detail("Kopi", 1, "Drinks", sku, sugar);
        OrderDetail food = detail("Roti", 2, "Food",
                sku(200, "Slice", 2, 12000, 24000), null);

        Order order = new Order();
        set(Order.class, order, "orderNo", "ORD-1");
        set(Order.class, order, "storeServiceTypesId", 1);
        set(Order.class, order, "orderDetails", Arrays.asList(drinks, food));
        return order;
    }

    private static OrderDetailCustomization customization(int skuId, String group, String option) {
        OrderDetailCustomization c = new OrderDetailCustomization();
        set(OrderDetailCustomization.class, c, "orderDetailSkuId", skuId);
        set(OrderDetailCustomization.class, c, "customizationName", group);
        set(OrderDetailCustomization.class, c, "options",
                Collections.singletonList(option(option)));
        return c;
    }

    private static OrderCustomizationOption option(String name) {
        OrderCustomizationOption o = new OrderCustomizationOption();
        set(OrderCustomizationOption.class, o, "customizationOptionName", name);
        set(OrderCustomizationOption.class, o, "quantity", 1);
        set(OrderCustomizationOption.class, o, "price", 0.0);
        set(OrderCustomizationOption.class, o, "amount", 0.0);
        return o;
    }

    private static OrderDetailSku sku(int id, String name, int quantity, double price, double amount) {
        OrderDetailSku s = new OrderDetailSku();
        set(OrderDetailSku.class, s, "id", id);
        set(OrderDetailSku.class, s, "skuName", name);
        setDouble(OrderDetailSku.class, s, "quantity", quantity);
        setDouble(OrderDetailSku.class, s, "price", price);
        setDouble(OrderDetailSku.class, s, "amount", amount);
        return s;
    }

    private static OrderDetail detail(String product, int categoryId, String categoryName,
                                      OrderDetailSku sku, OrderDetailCustomization customization) {
        OrderDetail d = new OrderDetail();
        set(OrderDetail.class, d, "productName", product);
        set(OrderDetail.class, d, "categoryId", categoryId);
        set(OrderDetail.class, d, "categoryName", categoryName);
        set(OrderDetail.class, d, "orderDetailSkus", Collections.singletonList(sku));
        if (customization != null) {
            set(OrderDetail.class, d, "customizations", Collections.singletonList(customization));
        }
        return d;
    }

    private static void set(Class<?> clazz, Object target, String field, Object value) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setDouble(Class<?> clazz, Object target, String field, double value) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            f.setDouble(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
