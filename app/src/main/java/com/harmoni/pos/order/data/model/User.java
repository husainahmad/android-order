package com.harmoni.pos.order.data.model;

public class User {
    private int id;
    private String username;
    private Store store;

    public int getId() { return id; }
    public String getUsername() { return username == null ? "" : username; }
    public Store getStore() { return store; }

    public String getStoreName() { return store != null ? store.getName() : ""; }
    public String getChainName() { return store != null && store.getChain() != null ? store.getChain().getName() : ""; }
    public String getBrandName() { return store != null && store.getChain() != null && store.getChain().getBrand() != null ? store.getChain().getBrand().getName() : ""; }
}
