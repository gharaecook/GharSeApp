package com.gharse.auth;

import com.gharse.auth.AuthDtos.AuthResponse;
import com.gharse.auth.AuthDtos.ForgotPasswordRequest;
import com.gharse.auth.AuthDtos.ForgotPasswordResponse;
import com.gharse.auth.AuthDtos.LoginRequest;
import com.gharse.auth.AuthDtos.RefreshRequest;
import com.gharse.auth.AuthDtos.RegisterRequest;
import com.gharse.auth.AuthDtos.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /** Creates a CUSTOMER or COOK account. New cooks start with approval status PENDING. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        return service.register(r);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        return service.login(r);
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest r) {
        return service.forgotPassword(r.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest r) {
        service.resetPassword(r.token(), r.newPassword());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest r) {
        return service.refresh(r.refreshToken());
    }
}
