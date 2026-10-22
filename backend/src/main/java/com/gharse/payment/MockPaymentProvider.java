package com.gharse.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Always succeeds. For the MVP and tests only; no real money moves. */
@Component
@ConditionalOnProperty(prefix = "gharse.payment", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String name() {
        return "MOCK";
    }

    @Override
    public Result charge(String orderNumber, BigDecimal amount) {
        return new Result(PaymentStatus.PAID, null, "mock_pay_" + UUID.randomUUID().toString().substring(0, 12), null);
    }

    @Override
    public boolean verifyPayment(String providerOrderId, String providerPaymentId, String signature) {
        return false;
    }

    @Override
    public String checkoutKeyId() {
        return null;
    }

    @Override
    public Result refund(String providerPaymentId, BigDecimal amount) {
        return new Result(PaymentStatus.REFUNDED, null, providerPaymentId, null);
    }
}
