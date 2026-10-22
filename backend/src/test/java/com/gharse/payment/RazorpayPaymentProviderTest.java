package com.gharse.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class RazorpayPaymentProviderTest {

    @Test
    void verifiesRazorpayCheckoutSignatureAndRejectsMismatch() throws Exception {
        String secret = "test-key-secret";
        String orderId = "order_123";
        String paymentId = "pay_456";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));

        assertThat(RazorpayPaymentProvider.verifySignature(secret, orderId, paymentId, signature)).isTrue();
        assertThat(RazorpayPaymentProvider.verifySignature(secret, orderId, paymentId, "not-a-signature")).isFalse();
        assertThat(RazorpayPaymentProvider.verifySignature(secret, orderId, "pay_other", signature)).isFalse();
    }
}
