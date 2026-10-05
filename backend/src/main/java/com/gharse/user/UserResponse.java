package com.gharse.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, String phone, UserRole role, UserStatus status,
                           String address, UUID cookId, Instant createdAt) {

    public static UserResponse of(User u, UUID cookId) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.getStatus(),
                u.getAddress(), cookId, u.getCreatedAt());
    }
}
