package com.gharse.payment;

public record PaymentCheckout(String provider, String keyId, String orderId, long amount, String currency) {}
