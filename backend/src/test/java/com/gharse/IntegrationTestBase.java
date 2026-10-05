package com.gharse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gharse.user.User;
import com.gharse.user.UserRepository;
import com.gharse.user.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Full-stack tests against a real PostgreSQL (Testcontainers), so Flyway migrations and constraints are exercised.
 * Tests share one database, so every test creates its own uniquely named users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    static final String PASSWORD = "Passw0rd!x";
    static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    /** One PostgreSQL container shared by the whole test run (started once, stopped by Ryuk when the JVM exits). */
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        PG.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository users;
    @Autowired protected PasswordEncoder encoder;

    public record Acct(String token, UUID userId, UUID cookId, String email, String name) {}

    // ------------------------------------------------------------------ HTTP helpers

    protected JsonNode call(HttpMethod method, String path, String token, Object body, int expectedStatus) {
        try {
            MockHttpServletRequestBuilder b = request(method, path).accept(MediaType.APPLICATION_JSON);
            if (token != null) b.header("Authorization", "Bearer " + token);
            if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
            String content = mvc.perform(b).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
            return content.isBlank() ? json.createObjectNode() : json.readTree(content);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected JsonNode get(String path, String token) { return call(HttpMethod.GET, path, token, null, 200); }
    protected JsonNode get(String path, String token, int status) { return call(HttpMethod.GET, path, token, null, status); }
    protected JsonNode post(String path, String token, Object body) { return call(HttpMethod.POST, path, token, body, 200); }
    protected JsonNode post(String path, String token, Object body, int status) { return call(HttpMethod.POST, path, token, body, status); }
    protected JsonNode put(String path, String token, Object body) { return call(HttpMethod.PUT, path, token, body, 200); }
    protected JsonNode put(String path, String token, Object body, int status) { return call(HttpMethod.PUT, path, token, body, status); }
    protected JsonNode delete(String path, String token, int status) { return call(HttpMethod.DELETE, path, token, null, status); }

    // ------------------------------------------------------------------ accounts

    protected static String unique() { return UUID.randomUUID().toString().substring(0, 8); }

    protected Acct registerCustomer() {
        return register("CUSTOMER", "Cust " + unique());
    }

    protected Acct registerCook(String name) {
        return register("COOK", name);
    }

    private Acct register(String role, String name) {
        String email = unique() + "@test.gharse";
        JsonNode r = post("/api/v1/auth/register", null, Map.of("name", name, "phone", "9876543210", "email", email,
                "password", PASSWORD, "role", role, "locality", "Kothrud"), 201);
        String cookId = r.path("user").path("cookId").asText(null);
        return new Acct(r.get("accessToken").asText(), UUID.fromString(r.path("user").get("id").asText()),
                cookId == null ? null : UUID.fromString(cookId), email, name);
    }

    protected Acct admin() {
        User u = new User();
        u.setName("Admin " + unique());
        u.setPhone("9000000000");
        u.setEmail(unique() + "@admin.gharse");
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setRole(UserRole.ADMIN);
        users.save(u);
        JsonNode r = post("/api/v1/auth/login", null, Map.of("email", u.getEmail(), "password", PASSWORD));
        return new Acct(r.get("accessToken").asText(), u.getId(), null, u.getEmail(), u.getName());
    }

    /** A cook who has been approved by an admin. */
    protected Acct approvedCook() {
        return approvedCook("Cook " + unique());
    }

    protected Acct approvedCook(String name) {
        Acct cook = registerCook(name);
        post("/api/v1/admin/cooks/" + cook.cookId() + "/approve", admin().token(), null);
        return cook;
    }

    // ------------------------------------------------------------------ catalogue helpers

    protected UUID createDish(Acct cook, String name, double price) {
        JsonNode d = post("/api/v1/cook/dishes", cook.token(), Map.of("name", name, "category", "MEAL", "price", price,
                "portionDescription", "1 plate", "vegetarian", true), 201);
        return UUID.fromString(d.get("id").asText());
    }

    protected LocalDate tomorrow() { return LocalDate.now(ZONE).plusDays(1); }

    protected Map<String, Object> batchBody(UUID dishId, int capacity, LocalDate date, Instant cutoff) {
        return Map.of("name", "Sunday Special", "description", "Test batch", "availableDate", date.toString(),
                "orderCutoff", cutoff.toString(), "capacity", capacity,
                "pickupStart", "12:00", "pickupEnd", "14:00", "deliveryStart", "12:00", "deliveryEnd", "15:00",
                "dishes", List.of(Map.of("dishId", dishId.toString())));
    }

    /** Creates an open batch for tomorrow; returns its id. */
    protected UUID createBatch(Acct cook, UUID dishId, int capacity) {
        JsonNode b = post("/api/v1/cook/batches", cook.token(),
                batchBody(dishId, capacity, tomorrow(), Instant.now().plus(10, ChronoUnit.HOURS)), 201);
        return UUID.fromString(b.get("id").asText());
    }

    protected JsonNode addToCart(Acct customer, UUID dishId, UUID batchId, int qty, int status) {
        return post("/api/v1/cart/items", customer.token(), Map.of("dishId", dishId.toString(), "batchId", batchId.toString(),
                "quantity", qty), status);
    }

    protected JsonNode checkout(Acct customer, String type, int status) {
        Map<String, Object> body = type.equals("PICKUP") ? Map.of("orderType", type)
                : Map.of("orderType", type, "deliveryAddress", "12 Test Road, Pune");
        return post("/api/v1/orders", customer.token(), body, status);
    }

    /** Customer orders {@code qty} of the dish in the batch (pickup) and returns the order JSON. */
    protected JsonNode placeOrder(Acct customer, UUID dishId, UUID batchId, int qty) {
        addToCart(customer, dishId, batchId, qty, 200);
        return checkout(customer, "PICKUP", 201);
    }

    protected JsonNode batchOf(UUID cookId, UUID batchId) {
        for (JsonNode b : get("/api/v1/cooks/" + cookId + "/batches", null)) {
            if (b.get("id").asText().equals(batchId.toString())) return b;
        }
        throw new AssertionError("batch not found in public listing");
    }
}
