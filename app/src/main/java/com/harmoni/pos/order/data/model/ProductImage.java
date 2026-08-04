package com.harmoni.pos.order.data.model;

public class ProductImage {
    private int id;
    private String fileName;
    private String imageBlob;
    private String mimeType;

    public int getId() { return id; }
    public String getFileName() { return fileName == null ? "" : fileName; }
    public String getImageBlob() { return imageBlob == null ? "" : imageBlob; }
    public String getMimeType() { return mimeType == null ? "" : mimeType; }
}
