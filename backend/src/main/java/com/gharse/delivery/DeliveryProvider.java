package com.gharse.delivery;

import java.math.BigDecimal;

/**
 * Third-party delivery abstraction. Future implementations: PorterDeliveryProvider, BorzoDeliveryProvider,
 * ShiprocketDeliveryProvider. Only {@link MockDeliveryProvider} exists in the MVP.
 */
public interface DeliveryProvider {

    String name();

    BigDecimal quote(Request request);

    Booking book(Request request);

    void cancel(String providerOrderId);

    record Request(String orderNumber, String pickupAddress, String dropAddress) {}

    record Booking(String providerOrderId, String trackingUrl, BigDecimal cost) {}
}
