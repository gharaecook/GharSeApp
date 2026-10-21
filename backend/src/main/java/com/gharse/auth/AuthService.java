package com.gharse.auth;

import com.gharse.auth.AuthDtos.AuthResponse;
import com.gharse.auth.AuthDtos.ForgotPasswordResponse;
import com.gharse.auth.AuthDtos.LoginRequest;
import com.gharse.auth.AuthDtos.RegisterRequest;
import com.gharse.common.AppProperties;
import com.gharse.common.ApiException;
import com.gharse.cook.Cook;
import com.gharse.cook.CookRepository;
import com.gharse.cook.CookService;
import com.gharse.user.User;
import com.gharse.user.UserResponse;
import com.gharse.user.UserRole;
import com.gharse.user.UserStatus;
import com.gharse.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository users;
    private final CookRepository cooks;
    private final CookService cookService;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final PasswordResetTokenRepository resetTokens;
    private final boolean exposeResetToken;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users, CookRepository cooks, CookService cookService, PasswordEncoder encoder,
            JwtService jwt, PasswordResetTokenRepository resetTokens, AppProperties properties) {
        this.users = users;
        this.cooks = cooks;
        this.cookService = cookService;
        this.encoder = encoder;
        this.jwt = jwt;
        this.resetTokens = resetTokens;
        this.exposeResetToken = properties.auth().exposeResetToken();
    }

    public AuthResponse register(RegisterRequest r) {
        if (r.role() == UserRole.ADMIN) throw ApiException.forbidden("Admin accounts cannot be self-registered");
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account with this email already exists");
        }
        User u = new User();
        u.setName(r.name().trim());
        u.setPhone(r.phone().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(r.role());
        u = users.save(u);
        UUID cookId = null;
        if (r.role() == UserRole.COOK) {
            cookId = cookService.createForUser(u, r.locality()).getId();
        }
        return tokens(u, cookId);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim()).orElse(null);
        // Same message for unknown email and wrong password: do not reveal which accounts exist.
        if (u == null || !encoder.matches(r.password(), u.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        if (u.getStatus() != UserStatus.ACTIVE) throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "This account is suspended");
        return tokens(u, cookIdOf(u));
    }

    public ForgotPasswordResponse forgotPassword(String email) {
        String resetToken = null;
        resetTokens.deleteByExpiresAtBefore(Instant.now());
        User user = users.findByEmail(email.trim()).orElse(null);
        if (user != null) {
            resetTokens.deleteByUser(user);
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);
            resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            PasswordResetToken entity = new PasswordResetToken();
            entity.setUser(user);
            entity.setTokenHash(hashToken(resetToken));
            entity.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            resetTokens.save(entity);
        }
        return new ForgotPasswordResponse(
                "If an account exists for that email, password reset instructions will be provided.",
                exposeResetToken ? resetToken : null);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokens
                .findValidToken(hashToken(token), Instant.now())
                .orElseThrow(() -> ApiException.badRequest("INVALID_RESET_TOKEN", "This password reset link is invalid or has expired"));
        User user = resetToken.getUser();
        user.setPasswordHash(encoder.encode(newPassword));
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        users.save(user);
        resetTokens.deleteByUser(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        AuthUser au = jwt.parseRefresh(refreshToken).orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        User u = users.findById(au.id()).orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (u.getStatus() != UserStatus.ACTIVE || au.credentialVersion() != u.getCredentialVersion()) {
            throw ApiException.unauthorized("Invalid refresh token");
        }
        return tokens(u, cookIdOf(u));
    }

    private UUID cookIdOf(User u) {
        return u.getRole() == UserRole.COOK ? cooks.findByUserId(u.getId()).map(Cook::getId).orElse(null) : null;
    }

    private AuthResponse tokens(User u, UUID cookId) {
        return new AuthResponse(jwt.accessToken(u), jwt.refreshToken(u), "Bearer", jwt.accessTokenSeconds(), UserResponse.of(u, cookId));
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
