package com.gharse.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gharse.common.ApiException;
import com.gharse.common.AppProperties;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "gharse.payment", name = "provider", havingValue = "razorpay")
public class RazorpayPaymentProvider implements PaymentProvider {

    private final RestClient client;
    private final String keyId;
    private final String keySecret;

    public RazorpayPaymentProvider(RestClient.Builder builder, AppProperties properties) {
        keyId = properties.payment().razorpayKeyId();
        keySecret = properties.payment().razorpayKeySecret();
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException("RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET are required when PAYMENT_PROVIDER=razorpay");
        }
        client = builder.baseUrl("https://api.razorpay.com/v1")
                .defaultHeaders(headers -> headers.setBasicAuth(keyId, keySecret))
                .build();
    }

    @Override
    public String name() {
        return "RAZORPAY";
    }

    @Override
    public Result charge(String orderNumber, BigDecimal amount) {
        long amountInPaise = amount.movePointRight(2).longValueExact();
        try {
            GatewayOrder order = client.post()
                    .uri("/orders")
                    .body(Map.of("amount", amountInPaise, "currency", "INR", "receipt", orderNumber))
                    .retrieve()
                    .body(GatewayOrder.class);
            if (order == null || order.id() == null || order.id().isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER_ERROR", "Payment provider returned an invalid order");
            }
            return new Result(PaymentStatus.PENDING, order.id(), null, null);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER_ERROR", "Could not start payment. Please try again.");
        }
    }

    @Override
    public boolean verifyPayment(String providerOrderId, String providerPaymentId, String signature) {
        return verifySignature(keySecret, providerOrderId, providerPaymentId, signature);
    }

    static boolean verifySignature(String keySecret, String providerOrderId, String providerPaymentId, String signature) {
        if (providerOrderId == null || providerPaymentId == null || signature == null || signature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((providerOrderId + "|" + providerPaymentId).getBytes(StandardCharsets.UTF_8));
            return java.security.MessageDigest.isEqual(
                    HexFormat.of().formatHex(digest).getBytes(StandardCharsets.US_ASCII),
                    signature.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Unable to verify Razorpay payment signature", e);
        }
    }

    @Override
    public String checkoutKeyId() {
        return keyId;
    }

    @Override
    public Result refund(String providerPaymentId, BigDecimal amount) {
        long amountInPaise = amount.movePointRight(2).longValueExact();
        try {
            GatewayRefund refund = client.post()
                    .uri("/payments/{paymentId}/refund", providerPaymentId)
                    .body(Map.of("amount", amountInPaise))
                    .retrieve()
                    .body(GatewayRefund.class);
            if (refund == null || refund.id() == null || refund.id().isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER_ERROR", "Payment provider returned an invalid refund");
            }
            PaymentStatus status = switch (refund.status() == null ? "" : refund.status().toLowerCase(java.util.Locale.ROOT)) {
                case "processed" -> PaymentStatus.REFUNDED;
                case "pending" -> PaymentStatus.PENDING;
                case "failed" -> PaymentStatus.FAILED;
                default -> throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER_ERROR",
                        "Payment provider returned an unknown refund status");
            };
            return new Result(status, null, providerPaymentId, refund.id());
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER_ERROR", "Could not refund payment. Please try again.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GatewayOrder(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GatewayRefund(String id, String status) {}
}
