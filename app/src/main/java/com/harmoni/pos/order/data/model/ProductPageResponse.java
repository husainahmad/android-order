package com.harmoni.pos.order.data.model;

import java.util.ArrayList;
import java.util.List;

/** One page of products plus the cursor for the next one. */
public class ProductPageResponse {
    private List<Product> products;
    private Integer nextCursor;
    private Boolean hasMore;

    public List<Product> getProducts() {
        return products == null ? new ArrayList<>() : products;
    }

    public Integer getNextCursor() { return nextCursor; }

    public boolean hasMore() {
        if (hasMore != null) return hasMore;
        return nextCursor != null;
    }
}
