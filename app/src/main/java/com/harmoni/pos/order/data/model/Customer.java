package com.harmoni.pos.order.data.model;

/**
 * A customer as the customer service returns it. {@code id} is what an order carries in
 * {@code customerId} so the sale can be traced back to a real person rather than a name
 * typed at the till.
 */
public class Customer {
    private long id;
    private String name;
    private String phone;
    private String email;
    private String createdAt;
    private String updatedAt;

    public long getId() { return id; }
    public String getName() { return name == null ? "" : name; }
    public String getPhone() { return phone == null ? "" : phone; }
    public String getEmail() { return email == null ? "" : email; }
    public String getCreatedAt() { return createdAt == null ? "" : createdAt; }
    public String getUpdatedAt() { return updatedAt == null ? "" : updatedAt; }

    public String getNameValue() { return getName().trim(); }
    public String getPhoneValue() { return getPhone().trim(); }
    public String getEmailValue() { return getEmail().trim(); }

    /** What a cashier recognises a customer by: name, then phone, then id. */
    public String getDisplayName() {
        if (!getNameValue().isEmpty()) return getNameValue();
        if (!getPhoneValue().isEmpty()) return getPhoneValue();
        return "Customer #" + id;
    }
}
