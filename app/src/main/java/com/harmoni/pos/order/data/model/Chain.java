package com.harmoni.pos.order.data.model;

public class Chain {
    private int id;
    private String name;
    private Brand brand;

    public int getId() { return id; }
    public String getName() { return name == null ? "" : name; }
    public Brand getBrand() { return brand; }
}
