package com.harmoni.pos.order.data.model;

public class Category {
    private int id;
    private String name;

    public int getId() { return id; }
    public String getName() { return name == null ? "" : name; }
}
