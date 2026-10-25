package com.gharse.user;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(UUID id, String name, String email, String phone, UserRole role, UserStatus status,
                                  String address, String society, String area, String landmark, String formattedAddress,
                                  Double deliveryLatitude, Double deliveryLongitude, UUID cookId, Instant createdAt) {

    public static UserProfileResponse of(User user, UUID cookId) {
        return new UserProfileResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole(),
                user.getStatus(), user.getAddress(), user.getSociety(), user.getArea(), user.getLandmark(),
                user.deliveryAddress(), user.getDeliveryLatitude(), user.getDeliveryLongitude(), cookId, user.getCreatedAt());
    }
}
