package com.harmoni.pos.order.ui.orderform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.data.model.TierPrice;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

public class OrderFormViewModelTest {

    @Before
    public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(Runnable runnable) {
                runnable.run();
            }

            @Override
            public void postToMainThread(Runnable runnable) {
                runnable.run();
            }

            @Override
            public boolean isMainThread() {
                return true;
            }
        });
    }

    @Test
    public void ensureActiveTabCreatesFirstOrder() {
        OrderFormViewModel vm = new OrderFormViewModel();
        vm.ensureActiveTab();
        assertEquals(1, vm.getTabs().getValue().size());
        assertEquals("#001", vm.getActiveTab().label);
        assertEquals(1, vm.getActiveTab().id);
    }

    @Test
    public void createNewTabAddsNumberedTabsAndActivatesNewest() {
        OrderFormViewModel vm = new OrderFormViewModel();
        vm.ensureActiveTab();
        vm.createNewTab();
        vm.createNewTab();
        List<OrderFormViewModel.OrderTab> tabs = vm.getTabs().getValue();
        assertEquals(3, tabs.size());
        assertEquals("#001", tabs.get(0).label);
        assertEquals("#002", tabs.get(1).label);
        assertEquals("#003", tabs.get(2).label);
        assertEquals(3, vm.getActiveTab().id);
    }

    @Test
    public void eachTabKeepsItsOwnCart() throws Exception {
        OrderFormViewModel vm = new OrderFormViewModel();
        vm.ensureActiveTab();
        vm.createNewTab();

        Product hazelnut = product(1, "Hazelnut", 1);
        Sku hot = sku(11, "Hot Regular", 17000);

        vm.switchTab(1);
        vm.addToCart(hazelnut, hot);
        vm.addToCart(hazelnut, hot);
        assertEquals(1, vm.getCart().getValue().size());
        assertEquals(2, vm.getCart().getValue().get(0).getQuantity());
        assertEquals(34000, vm.getSubtotal(), 0.001);

        vm.switchTab(2);
        assertNotNull(vm.getCart().getValue());
        assertEquals(0, vm.getCart().getValue().size());
        assertEquals(0, vm.getSubtotal(), 0.001);

        vm.switchTab(1);
        assertEquals(1, vm.getCart().getValue().size());
        assertEquals(34000, vm.getSubtotal(), 0.001);
    }

    @Test
    public void eachTabKeepsItsOwnCustomerDiscountRemark() {
        OrderFormViewModel vm = new OrderFormViewModel();
        vm.ensureActiveTab();
        vm.createNewTab();

        vm.switchTab(1);
        vm.setCustomer("Budi");
        vm.setDiscount("1000");
        vm.setRemark("no ice");

        vm.switchTab(2);
        vm.setCustomer("Sari");

        vm.switchTab(1);
        assertEquals("Budi", vm.getActiveTab().customer);
        assertEquals("1000", vm.getActiveTab().discount);
        assertEquals("no ice", vm.getActiveTab().remark);
        assertEquals(1000, vm.getActiveDiscount(), 0.001);

        vm.switchTab(2);
        assertEquals("Sari", vm.getActiveTab().customer);
        assertEquals("0", vm.getActiveTab().discount);
    }

    @Test
    public void removeActiveTabMovesToNextTabOrReturnsMinusOne() {
        OrderFormViewModel vm = new OrderFormViewModel();
        vm.ensureActiveTab();
        vm.createNewTab();
        vm.createNewTab();
        vm.switchTab(2);

        int next = vm.removeActiveTab();
        assertEquals(3, next);
        assertEquals(2, vm.getTabs().getValue().size());
        assertEquals(3, vm.getActiveTab().id);

        next = vm.removeActiveTab();
        assertEquals(1, next);
        assertEquals(1, vm.getTabs().getValue().size());
        assertEquals(1, vm.getActiveTab().id);

        next = vm.removeActiveTab();
        assertEquals(-1, next);
        assertEquals(0, vm.getTabs().getValue().size());
        assertNull(vm.getActiveTab());
    }

    @Test
    public void removeActiveTabClearsConfirmedOrder() {
        OrderFormViewModel vm = new OrderFormViewModel();
        vm.ensureActiveTab();
        vm.removeActiveTab();
        assertNull(vm.getConfirmedOrder().getValue());
    }

    private static Product product(int id, String name, int categoryId) throws Exception {
        Product p = new Product();
        setInt(Product.class, p, "id", id);
        set(Product.class, p, "name", name);
        setInt(Product.class, p, "categoryId", categoryId);
        return p;
    }

    private static Sku sku(int id, String name, double price) throws Exception {
        Sku s = new Sku();
        setInt(Sku.class, s, "id", id);
        set(Sku.class, s, "name", name);
        TierPrice tierPrice = new TierPrice();
        setDouble(TierPrice.class, tierPrice, "price", price);
        set(Sku.class, s, "tierPrice", tierPrice);
        return s;
    }

    private static void set(Class<?> clazz, Object target, String field, Object value) throws Exception {
        Field f = clazz.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void setInt(Class<?> clazz, Object target, String field, int value) throws Exception {
        Field f = clazz.getDeclaredField(field);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    private static void setDouble(Class<?> clazz, Object target, String field, double value) throws Exception {
        Field f = clazz.getDeclaredField(field);
        f.setAccessible(true);
        f.setDouble(target, value);
    }
}
