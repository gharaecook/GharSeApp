package com.gharse.delivery;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Flat-fee fake courier used until a real provider is integrated. */
@Component
public class MockDeliveryProvider implements DeliveryProvider {

    private static final BigDecimal FLAT_FEE = new BigDecimal("60.00");

    @Override
    public String name() {
        return "MOCK";
    }

    @Override
    public BigDecimal quote(Request request) {
        return FLAT_FEE;
    }

    @Override
    public Booking book(Request request) {
        String id = "mock_dlv_" + UUID.randomUUID().toString().substring(0, 10);
        return new Booking(id, "https://tracking.gharse.invalid/" + id, FLAT_FEE);
    }

    @Override
    public void cancel(String providerOrderId) {
        // nothing to cancel for the mock
    }
}
