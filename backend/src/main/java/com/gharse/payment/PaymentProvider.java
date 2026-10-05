package com.gharse.payment;

import java.math.BigDecimal;

/**
 * Payment gateway abstraction. OrderService only ever talks to {@link PaymentService}, which talks to this interface,
 * so Razorpay (or any gateway) is a new implementation, not a change to order logic.
 */
public interface PaymentProvider {

    String name();

    Result charge(String orderNumber, BigDecimal amount);

    Result refund(String providerPaymentId, BigDecimal amount);

    record Result(PaymentStatus status, String providerPaymentId) {}
}
