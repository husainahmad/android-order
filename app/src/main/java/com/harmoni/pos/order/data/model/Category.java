package com.harmoni.pos.order.data.model;

public class Category {
    private int id;
    private String name;
    private int iconRes;

    public int getId() { return id; }
    public String getName() { return name == null ? "" : name; }
    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
}
