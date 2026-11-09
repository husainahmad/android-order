package com.harmoni.pos.order.data.model;

import java.util.List;

public class OrderDetailRequest {
    private int productId;
    private String productName;
    private int categoryId;
    /**
     * Kitchen instruction for this product group. Lines of the same product with
     * different notes travel as separate groups, because the order service stores
     * the note on the order detail, not on the SKU line.
     */
    private String remark;
    private List<OrderDetailSkuRequest> orderDetailSkus;

    public OrderDetailRequest(int productId, String productName, int categoryId,
                              List<OrderDetailSkuRequest> orderDetailSkus) {
        this(productId, productName, categoryId, orderDetailSkus, null);
    }

    public OrderDetailRequest(int productId, String productName, int categoryId,
                              List<OrderDetailSkuRequest> orderDetailSkus, String remark) {
        this.productId = productId;
        this.productName = productName;
        this.categoryId = categoryId;
        this.remark = remark == null || remark.trim().isEmpty() ? null : remark.trim();
        this.orderDetailSkus = orderDetailSkus;
    }

    public String getRemark() { return remark; }
}
