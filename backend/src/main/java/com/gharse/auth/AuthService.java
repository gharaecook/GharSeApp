package com.gharse.auth;

import com.gharse.auth.AuthDtos.AuthResponse;
import com.gharse.auth.AuthDtos.LoginRequest;
import com.gharse.auth.AuthDtos.RegisterRequest;
import com.gharse.common.ApiException;
import com.gharse.cook.Cook;
import com.gharse.cook.CookRepository;
import com.gharse.cook.CookService;
import com.gharse.user.User;
import com.gharse.user.UserResponse;
import com.gharse.user.UserRole;
import com.gharse.user.UserStatus;
import com.gharse.user.UserRepository;
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

    public AuthService(UserRepository users, CookRepository cooks, CookService cookService, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.cooks = cooks;
        this.cookService = cookService;
        this.encoder = encoder;
        this.jwt = jwt;
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

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        AuthUser au = jwt.parseRefresh(refreshToken).orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        User u = users.findById(au.id()).orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (u.getStatus() != UserStatus.ACTIVE) throw ApiException.unauthorized("Invalid refresh token");
        return tokens(u, cookIdOf(u));
    }

    private UUID cookIdOf(User u) {
        return u.getRole() == UserRole.COOK ? cooks.findByUserId(u.getId()).map(Cook::getId).orElse(null) : null;
    }

    private AuthResponse tokens(User u, UUID cookId) {
        return new AuthResponse(jwt.accessToken(u), jwt.refreshToken(u), "Bearer", jwt.accessTokenSeconds(), UserResponse.of(u, cookId));
    }
}
