package com.harmoni.pos.order.data.model;

public class Store {
    private int id;
    private String name;
    private Chain chain;

    public int getId() { return id; }
    public String getName() { return name == null ? "" : name; }
    public Chain getChain() { return chain; }
}
