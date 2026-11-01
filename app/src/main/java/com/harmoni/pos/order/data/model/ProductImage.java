package com.harmoni.pos.order.data.model;

public class ProductImage {
    private int id;
    private String fileName;
    private String url;
    private String mimeType;

    public int getId() { return id; }
    public String getFileName() { return fileName == null ? "" : fileName; }

    /** Hosted image location. The menu service uploads to ImgBB and stores the link. */
    public String getUrl() { return url == null ? "" : url; }

    public String getMimeType() { return mimeType == null ? "" : mimeType; }
}
