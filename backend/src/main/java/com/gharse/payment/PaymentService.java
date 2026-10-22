package com.gharse.payment;

import com.gharse.common.ApiException;
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
    public ChargeResult charge(UUID orderId, String orderNumber, BigDecimal amount) {
        PaymentProvider.Result r = provider.charge(orderNumber, amount);
        Payment payment = save(orderId, amount, r);
        PaymentCheckout checkout = payment.getProviderOrderId() == null ? null
                : new PaymentCheckout(provider.name(), provider.checkoutKeyId(), payment.getProviderOrderId(),
                        amount.movePointRight(2).longValueExact(), "INR");
        return new ChargeResult(r.status(), checkout);
    }

    @Transactional(readOnly = true)
    public PaymentCheckout checkoutForOrder(UUID orderId) {
        return payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING && p.getProviderOrderId() != null
                        && p.getProvider().equals(provider.name()))
                .map(p -> new PaymentCheckout(p.getProvider(), provider.checkoutKeyId(), p.getProviderOrderId(),
                        p.getAmount().movePointRight(2).longValueExact(), "INR"))
                .orElse(null);
    }

    @Transactional
    public PaymentStatus verify(UUID orderId, String providerOrderId, String providerPaymentId, String signature) {
        Payment payment = payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> ApiException.conflict("PAYMENT_NOT_FOUND", "No payment is available for this order"));
        if (!payment.getProvider().equals(provider.name())) {
            throw ApiException.conflict("PAYMENT_PROVIDER_MISMATCH", "Payment provider configuration changed for this order");
        }
        if (payment.getProviderOrderId() == null || !payment.getProviderOrderId().equals(providerOrderId)) {
            throw ApiException.badRequest("PAYMENT_ORDER_MISMATCH", "The payment does not match this order");
        }
        if (!provider.verifyPayment(providerOrderId, providerPaymentId, signature)) {
            throw ApiException.badRequest("PAYMENT_SIGNATURE_INVALID", "Payment verification failed");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            if (!providerPaymentId.equals(payment.getProviderPaymentId())) {
                throw ApiException.conflict("PAYMENT_ALREADY_COMPLETED", "A different payment was already recorded for this order");
            }
            return PaymentStatus.PAID;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.conflict("PAYMENT_NOT_PENDING", "This payment is no longer pending");
        }
        payment.setProviderPaymentId(providerPaymentId);
        payment.setStatus(PaymentStatus.PAID);
        return PaymentStatus.PAID;
    }

    @Transactional
    public void cancelPending(UUID orderId) {
        payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .ifPresent(p -> p.setStatus(PaymentStatus.FAILED));
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

    private Payment save(UUID orderId, BigDecimal amount, PaymentProvider.Result r) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setProvider(provider.name());
        p.setProviderOrderId(r.providerOrderId());
        p.setProviderPaymentId(r.providerPaymentId());
        p.setProviderRefundId(r.providerRefundId());
        p.setAmount(amount);
        p.setStatus(r.status());
        return payments.save(p);
    }

    public record ChargeResult(PaymentStatus status, PaymentCheckout checkout) {}
}
