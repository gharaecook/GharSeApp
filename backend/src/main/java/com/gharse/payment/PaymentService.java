package com.gharse.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records every gateway interaction as a Payment row. Never logs payment secrets. */
@Service
public class PaymentService {

    private final PaymentProvider provider;
    private final PaymentRepository payments;

    public PaymentService(PaymentProvider provider, PaymentRepository payments) {
        this.provider = provider;
        this.payments = payments;
    }

    @Transactional
    public PaymentStatus charge(UUID orderId, String orderNumber, BigDecimal amount) {
        PaymentProvider.Result r = provider.charge(orderNumber, amount);
        save(orderId, amount, r);
        return r.status();
    }

    /** Refunds the latest successful payment of the order. */
    @Transactional
    public PaymentStatus refund(UUID orderId, BigDecimal amount) {
        var paid = payments.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID).findFirst();
        if (paid.isEmpty()) return PaymentStatus.PENDING;
        PaymentProvider.Result r = provider.refund(paid.get().getProviderPaymentId(), amount);
        save(orderId, amount, r);
        return r.status();
    }

    private void save(UUID orderId, BigDecimal amount, PaymentProvider.Result r) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setProvider(provider.name());
        p.setProviderPaymentId(r.providerPaymentId());
        p.setAmount(amount);
        p.setStatus(r.status());
        payments.save(p);
    }
}
