package com.gharse.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Always succeeds. For the MVP and tests only; no real money moves. */
@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String name() {
        return "MOCK";
    }

    @Override
    public Result charge(String orderNumber, BigDecimal amount) {
        return new Result(PaymentStatus.PAID, "mock_pay_" + UUID.randomUUID().toString().substring(0, 12));
    }

    @Override
    public Result refund(String providerPaymentId, BigDecimal amount) {
        return new Result(PaymentStatus.REFUNDED, providerPaymentId);
    }
}
