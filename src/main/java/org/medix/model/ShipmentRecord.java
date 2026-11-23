package org.medix.model;

import java.time.Instant;

public class ShipmentRecord {
    private String id;
    private String orderId;
    private String paymentId;
    private String trackingNumber;
    private String carrier;
    private String shippingMethod;
    private String status;
    private String recipientName;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String labelUrl;
    private Instant estimatedDelivery;
    private Instant createdAt;
    private Instant updatedAt;

    public ShipmentRecord() {
    }

    public ShipmentRecord(String id, String orderId, String paymentId, String trackingNumber,
                          String carrier, String shippingMethod, String status,
                          String recipientName, String street, String city, String state,
                          String postalCode, String country, String labelUrl,
                          Instant estimatedDelivery, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.shippingMethod = shippingMethod;
        this.status = status;
        this.recipientName = recipientName;
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.labelUrl = labelUrl;
        this.estimatedDelivery = estimatedDelivery;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLabelUrl() {
        return labelUrl;
    }

    public void setLabelUrl(String labelUrl) {
        this.labelUrl = labelUrl;
    }

    public Instant getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(Instant estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
