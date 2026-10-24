package com.gharse.cook;

import com.gharse.user.User;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CookDtos {

    private CookDtos() {}

    /** Card shown in discovery lists. Contains no private data (no phone, no full address). */
    public record CookSummary(UUID id, String name, String headline, String locality, String city, String profilePhotoUrl,
                              Integer yearsOfCooking, BigDecimal rating, int reviewCount, long completedOrderCount, List<String> cuisines,
                              List<String> signatureDishes, String storySnippet, boolean availableToday,
                              Double distanceKm, boolean verified, Instant joinedAt) {}

    public record CookDetail(CookSummary summary, String bio, String kitchenDescription, List<String> kitchenPhotos,
                             boolean fssaiVerified, boolean kitchenVerified, Boolean favorite) {}

    public record CuisineRef(UUID id, String name) {}

    public record KitchenResponse(String address, String locality, Double latitude, Double longitude, String description,
                                  ApprovalStatus verificationStatus, List<String> photos) {}

    public record DocumentResponse(UUID id, DocumentType documentType, String documentNumber, String documentUrl,
                                   ApprovalStatus status, LocalDate expiryDate, Instant createdAt) {

        public static DocumentResponse of(CookDocument d) {
            return new DocumentResponse(d.getId(), d.getDocumentType(), d.getDocumentNumber(), d.getDocumentUrl(),
                    d.getStatus(), d.getExpiryDate(), d.getCreatedAt());
        }
    }

    /** The cook's own (or an admin's) full view of a profile. */
    public record CookProfileResponse(UUID id, UUID userId, String name, String email, String phone, String address,
                                      String headline, String bio, Integer yearsOfCooking, String locality, String city,
                                      String profilePhotoUrl, String signatureDishes, ApprovalStatus approvalStatus,
                                      String rejectionReason, BigDecimal rating, int reviewCount,
                                      List<CuisineRef> cuisines, KitchenResponse kitchen, List<DocumentResponse> documents,
                                      Instant createdAt) {

        public static CookProfileResponse of(Cook c, com.gharse.kitchen.Kitchen k, List<CookDocument> docs) {
            User u = c.getUser();
            KitchenResponse kr = k == null ? null : new KitchenResponse(k.getAddress(), k.getLocality(), k.getLatitude(),
                    k.getLongitude(), k.getDescription(), k.getVerificationStatus(), List.copyOf(k.getPhotos()));
            return new CookProfileResponse(c.getId(), u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getAddress(),
                    c.getHeadline(), c.getBio(), c.getYearsOfCooking(), c.getLocality(), c.getCity(), c.getProfilePhotoUrl(),
                    c.getSignatureDishes(), c.getApprovalStatus(), c.getRejectionReason(), c.getRating(), c.getReviewCount(),
                    c.getCuisines().stream().map(x -> new CuisineRef(x.getId(), x.getName()))
                            .sorted(java.util.Comparator.comparing(CuisineRef::name)).toList(),
                    kr, docs.stream().map(DocumentResponse::of).toList(), c.getCreatedAt());
        }
    }

    public record UpdateCookProfileRequest(
            @Size(max = 120) String name,
            @Pattern(regexp = "^[0-9+\\-\\s]{10,20}$", message = "must be a valid phone number") String phone,
            @Size(max = 500) String address,
            @Size(max = 200) String headline,
            @Size(max = 4000) String bio,
            @Min(0) @Max(80) Integer yearsOfCooking,
            @Size(max = 120) String locality,
            @Size(max = 80) String city,
            @Size(max = 500) String profilePhotoUrl,
            @Size(max = 500) String signatureDishes,
            List<UUID> cuisineIds,
            @Size(max = 500) String kitchenAddress,
            @Size(max = 120) String kitchenLocality,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @Size(max = 4000) String kitchenDescription,
            @Size(max = 10) List<@Size(max = 500) String> kitchenPhotos) {}

    public record DocumentRequest(@NotNull DocumentType documentType, @Size(max = 100) String documentNumber,
                                  @Size(max = 500) String documentUrl, LocalDate expiryDate) {}

    public record CookDashboard(ApprovalStatus approvalStatus, long todaysOrders, long upcomingOrders, long pendingActions,
                                long activeBatches, BigDecimal totalEarnings, BigDecimal earningsThisMonth) {}

    public record Earnings(BigDecimal totalEarnings, BigDecimal pendingEarnings, BigDecimal earningsToday,
                           BigDecimal earningsThisMonth, long completedOrders, List<EarningLine> recent) {}

    public record EarningLine(UUID orderId, String orderNumber, BigDecimal amount, Instant completedAt) {}

    public record RejectRequest(@NotBlank @Size(max = 500) String reason) {}
}
