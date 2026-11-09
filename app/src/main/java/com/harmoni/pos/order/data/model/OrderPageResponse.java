package com.harmoni.pos.order.data.model;

import java.util.ArrayList;
import java.util.List;

/** One page of orders plus the cursor for the next one. */
public class OrderPageResponse {
    private List<Order> orders;
    private Integer nextCursor;
    private Boolean hasMore;

    public List<Order> getOrders() { return orders == null ? new ArrayList<>() : orders; }
    public Integer getNextCursor() { return nextCursor; }
    public boolean hasMore() {
        if (hasMore != null) return hasMore;
        return nextCursor != null;
    }
}
