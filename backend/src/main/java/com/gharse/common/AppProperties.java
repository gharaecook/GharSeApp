package com.gharse.common;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the {@code gharse.*} configuration. */
@ConfigurationProperties(prefix = "gharse")
public record AppProperties(
        String frontendUrl,
        String timezone,
        Jwt jwt,
        Storage storage,
        Cook cook,
        Order order,
        RateLimit rateLimit,
        Seed seed,
        AdminBootstrap adminBootstrap,
        Auth auth,
        Payment payment) {

    public record Jwt(String secret, int accessTokenMinutes, int refreshTokenDays) {}

    public record Storage(String type, String localDir, String publicBaseUrl, String supabaseUrl,
                          String supabaseServiceKey, String publicBucket, String privateBucket, long maxFileBytes) {}

    public record Cook(boolean fssaiRequired) {}

    public record Order(BigDecimal cookDeliveryFee) {}

    public record RateLimit(boolean enabled, int authRequestsPerMinute) {}

    public record Seed(boolean enabled) {}

    public record AdminBootstrap(String email, String password) {}

    public record Auth(boolean exposeResetToken) {}

    public record Payment(String provider, String razorpayKeyId, String razorpayKeySecret) {}
}
