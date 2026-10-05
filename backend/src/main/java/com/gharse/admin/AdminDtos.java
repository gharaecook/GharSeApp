package com.gharse.admin;

import com.gharse.cook.ApprovalStatus;
import com.gharse.cook.Cook;
import com.gharse.cook.CookDtos.CookProfileResponse;
import com.gharse.menu.DishDtos.DishResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
}
