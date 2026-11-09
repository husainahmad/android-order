package com.harmoni.pos.order.data.model;

import java.util.ArrayList;
import java.util.List;

/** The paged envelope {@code GET /api/v1/customers} answers with. */
public class CustomerPage {
    private List<Customer> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer page;
    private Integer size;

    public List<Customer> getContent() { return content == null ? new ArrayList<>() : content; }
    public long getTotalElements() { return totalElements == null ? 0 : totalElements; }
    public int getTotalPages() { return totalPages == null ? 0 : totalPages; }
    public int getPage() { return page == null ? 0 : page; }
    public int getSize() { return size == null ? 0 : size; }
}
