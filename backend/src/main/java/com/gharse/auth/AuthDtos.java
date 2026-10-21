package com.gharse.auth;

import com.gharse.user.UserResponse;
import com.gharse.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    /** {@code role} must be CUSTOMER or COOK; admins are created out of band. */
    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "^[0-9+\\-\\s]{10,20}$", message = "must be a valid phone number") String phone,
            @NotBlank @Email @Size(max = 200) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password,
            @NotNull UserRole role,
            @Size(max = 120) String locality) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 200) String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String newPassword) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ForgotPasswordResponse(String message, String resetToken) {}

    public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, UserResponse user) {}
}
