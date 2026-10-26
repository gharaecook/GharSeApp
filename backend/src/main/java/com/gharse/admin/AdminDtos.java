package com.gharse.admin;

import com.gharse.cook.ApprovalStatus;
import com.gharse.cook.Cook;
import com.gharse.cook.CookDtos.CookProfileResponse;
import com.gharse.menu.DishDtos.DishResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public final class AdminDtos {

    private AdminDtos() {}

    public record DashboardResponse(long totalUsers, long totalCustomers, long totalCooks, long pendingCooks,
                                    long approvedCooks, long rejectedCooks, long totalOrders, long completedOrders,
                                    BigDecimal revenue) {}

    public record AdminCookRow(UUID id, UUID userId, String name, String email, String phone, String locality,
                               ApprovalStatus approvalStatus, java.math.BigDecimal rating, int reviewCount, Instant createdAt) {

        public static AdminCookRow of(Cook c) {
            return new AdminCookRow(c.getId(), c.getUser().getId(), c.getUser().getName(), c.getUser().getEmail(),
                    c.getUser().getPhone(), c.getLocality(), c.getApprovalStatus(), c.getRating(), c.getReviewCount(),
                    c.getCreatedAt());
        }
    }

    public record AdminCookDetail(CookProfileResponse profile, List<DishResponse> dishes) {}

    public record DeliveryAreaResponse(UUID id, String placeId, String name, String formattedAddress, String locality,
                                       String city, double latitude, double longitude, DeliveryAreaStatus status) {
        public static DeliveryAreaResponse of(DeliveryArea area) {
            return new DeliveryAreaResponse(area.getId(), area.getPlaceId(), area.getName(), area.getFormattedAddress(),
                    area.getLocality(), area.getCity(), area.getLatitude(), area.getLongitude(), area.getStatus());
        }
    }

    public record CreateDeliveryAreaRequest(
            @NotBlank @Size(max = 200) String placeId,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 500) String formattedAddress,
            @NotBlank @Size(max = 120) String locality,
            @NotBlank @Size(max = 120) String city,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            @NotNull DeliveryAreaStatus status) {}

    public record DeliveryAreaStatusRequest(@NotNull DeliveryAreaStatus status) {}
}
