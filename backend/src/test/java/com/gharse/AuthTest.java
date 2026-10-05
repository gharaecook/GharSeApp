package com.gharse;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class AuthTest extends IntegrationTestBase {

    @Test
    void registerCustomerReturnsTokensAndNeverExposesThePassword() {
        String email = unique() + "@test.gharse";
        JsonNode r = post("/api/v1/auth/register", null, Map.of("name", "Priya Sharma", "phone", "9876543210",
                "email", email, "password", PASSWORD, "role", "CUSTOMER"), 201);
        assertThat(r.get("accessToken").asText()).isNotBlank();
        assertThat(r.get("refreshToken").asText()).isNotBlank();
        assertThat(r.path("user").get("role").asText()).isEqualTo("CUSTOMER");
        assertThat(r.toString()).doesNotContain("password").doesNotContain(PASSWORD);
        // stored hashed, not in plain text
        assertThat(users.findByEmail(email).orElseThrow().getPasswordHash()).startsWith("$2").doesNotContain(PASSWORD);
    }

    @Test
    void registeringACookCreatesAPendingCookProfile() {
        Acct cook = registerCook("Meena " + unique());
        assertThat(cook.cookId()).isNotNull();
        JsonNode profile = get("/api/v1/cook/profile", cook.token());
        assertThat(profile.get("approvalStatus").asText()).isEqualTo("PENDING");
    }

    @Test
    void duplicateEmailIsRejected() {
        Acct a = registerCustomer();
        JsonNode err = post("/api/v1/auth/register", null, Map.of("name", "Dup", "phone", "9876543210",
                "email", a.email().toUpperCase(), "password", PASSWORD, "role", "CUSTOMER"), 409);
        assertThat(err.get("code").asText()).isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void validationErrorsUseTheConsistentErrorBody() {
        JsonNode err = post("/api/v1/auth/register", null, Map.of("name", "", "phone", "abc", "email", "not-an-email",
                "password", "short", "role", "CUSTOMER"), 400);
        assertThat(err.get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(err.get("status").asInt()).isEqualTo(400);
        assertThat(err.get("path").asText()).isEqualTo("/api/v1/auth/register");
        assertThat(err.has("timestamp")).isTrue();
        assertThat(err.get("errors").size()).isGreaterThanOrEqualTo(4);
        assertThat(err.toString()).doesNotContain("Exception").doesNotContain("at com.gharse");
    }

    @Test
    void adminAccountsCannotBeSelfRegistered() {
        post("/api/v1/auth/register", null, Map.of("name", "Evil", "phone", "9876543210", "email", unique() + "@x.test",
                "password", PASSWORD, "role", "ADMIN"), 403);
    }

    @Test
    void loginSucceedsWithCorrectPasswordAndFailsOtherwise() {
        Acct a = registerCustomer();
        JsonNode ok = post("/api/v1/auth/login", null, Map.of("email", a.email(), "password", PASSWORD));
        assertThat(ok.get("accessToken").asText()).isNotBlank();

        JsonNode bad = post("/api/v1/auth/login", null, Map.of("email", a.email(), "password", "wrong-password"), 401);
        JsonNode unknown = post("/api/v1/auth/login", null, Map.of("email", "nobody@test.gharse", "password", "wrong-password"), 401);
        assertThat(bad.get("message").asText()).isEqualTo(unknown.get("message").asText());
    }

    @Test
    void refreshTokenIssuesANewPairButAnAccessTokenCannotBeUsedToRefresh() {
        Acct a = registerCustomer();
        JsonNode login = post("/api/v1/auth/login", null, Map.of("email", a.email(), "password", PASSWORD));
        JsonNode refreshed = post("/api/v1/auth/refresh", null, Map.of("refreshToken", login.get("refreshToken").asText()));
        assertThat(refreshed.get("accessToken").asText()).isNotBlank();
        post("/api/v1/auth/refresh", null, Map.of("refreshToken", login.get("accessToken").asText()), 401);
        post("/api/v1/auth/refresh", null, Map.of("refreshToken", "garbage"), 401);
    }

    @Test
    void protectedEndpointsRequireAValidToken() {
        get("/api/v1/cart", null, 401);
        get("/api/v1/orders", null, 401);
        get("/api/v1/cart", "not.a.jwt", 401);
        call(HttpMethod.GET, "/api/v1/admin/dashboard", null, null, 401);
    }

    @Test
    void rolesAreEnforced() {
        Acct customer = registerCustomer();
        Acct cook = registerCook("Cook " + unique());
        Acct admin = admin();
        get("/api/v1/admin/dashboard", customer.token(), 403);
        get("/api/v1/admin/dashboard", cook.token(), 403);
        get("/api/v1/cook/profile", customer.token(), 403);
        get("/api/v1/cook/orders", admin.token(), 403);
        get("/api/v1/cart", cook.token(), 403);
        get("/api/v1/orders", cook.token(), 403);
        get("/api/v1/admin/dashboard", admin.token());
    }

    @Test
    void publicDiscoveryNeedsNoLogin() {
        get("/api/v1/cooks", null);
        get("/api/v1/cuisines", null);
        get("/api/v1/batches", null);
    }

    @Test
    void errorsNeverLeakStackTraces() {
        JsonNode err = get("/api/v1/cooks/" + java.util.UUID.randomUUID(), null, 404);
        assertThat(err.get("code").asText()).isEqualTo("NOT_FOUND");
        get("/api/v1/cooks/not-a-uuid", null, 400);
    }
}
