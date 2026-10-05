package com.gharse.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ReviewDtos {

    private ReviewDtos() {}

    /** {@code rating} is the overall 1-5 score; the four aspect ratings are optional. */
    public record ReviewRequest(
            @Min(1) @Max(5) int rating,
            @Min(1) @Max(5) Integer tasteRating,
            @Min(1) @Max(5) Integer hygieneRating,
            @Min(1) @Max(5) Integer packagingRating,
            @Min(1) @Max(5) Integer reliabilityRating,
            @Size(max = 2000) String comment) {}

    public record ReviewResponse(UUID id, UUID orderId, String customerName, int rating, Integer tasteRating,
                                 Integer hygieneRating, Integer packagingRating, Integer reliabilityRating, String comment,
                                 Instant createdAt) {

        public static ReviewResponse of(Review r) {
            return new ReviewResponse(r.getId(), r.getOrderId(), firstName(r.getCustomer().getName()), r.getRating(),
                    r.getTasteRating(), r.getHygieneRating(), r.getPackagingRating(), r.getReliabilityRating(),
                    r.getComment(), r.getCreatedAt());
        }

        private static String firstName(String name) {
            String n = name == null ? "" : name.trim();
            int i = n.indexOf(' ');
            return i > 0 ? n.substring(0, i) : n;
        }
    }
}
