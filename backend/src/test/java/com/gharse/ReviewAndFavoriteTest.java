package com.gharse;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewAndFavoriteTest extends IntegrationTestBase {

    private String completedOrder(Acct cook, UUID dish, UUID batch, Acct customer) {
        String id = placeOrder(customer, dish, batch, 1).get("id").asText();
        String base = "/api/v1/cook/orders/" + id;
        post(base + "/confirm", cook.token(), null);
        post(base + "/prepare", cook.token(), null);
        post(base + "/ready", cook.token(), null);
        post(base + "/complete", cook.token(), null);
        return id;
    }

    @Test
    void reviewRules() {
        Acct cook = approvedCook();
        UUID dish = createDish(cook, "Misal", 90);
        UUID batch = createBatch(cook, dish, 20);
        Acct c = registerCustomer();
        String pending = placeOrder(c, dish, batch, 1).get("id").asText();
        Map<String, Object> review = Map.of("rating", 5, "tasteRating", 5, "hygieneRating", 4, "packagingRating", 4,
                "reliabilityRating", 5, "comment", "Tasted like home");

        assertThat(post("/api/v1/orders/" + pending + "/review", c.token(), review, 409).get("code").asText()).isEqualTo("ORDER_NOT_COMPLETED");

        String done = completedOrder(cook, dish, batch, c);
        post("/api/v1/orders/" + done + "/review", registerCustomer().token(), review, 404);          // not their order
        post("/api/v1/orders/" + done + "/review", c.token(), Map.of("rating", 6), 400);              // out of range
        post("/api/v1/orders/" + done + "/review", c.token(), Map.of("rating", 0), 400);
        JsonNode created = post("/api/v1/orders/" + done + "/review", c.token(), review, 201);
        assertThat(created.get("rating").asInt()).isEqualTo(5);
        assertThat(post("/api/v1/orders/" + done + "/review", c.token(), review, 409).get("code").asText()).isEqualTo("REVIEW_ALREADY_EXISTS");
        assertThat(get("/api/v1/orders/" + done, c.token()).get("reviewed").asBoolean()).isTrue();

        JsonNode detail = get("/api/v1/cooks/" + cook.cookId(), null).get("summary");
        assertThat(detail.get("rating").asDouble()).isEqualTo(5.0);
        assertThat(detail.get("reviewCount").asInt()).isEqualTo(1);

        // second customer lowers the average
        Acct c2 = registerCustomer();
        String done2 = completedOrder(cook, dish, batch, c2);
        post("/api/v1/orders/" + done2 + "/review", c2.token(), Map.of("rating", 4), 201);
        JsonNode after = get("/api/v1/cooks/" + cook.cookId(), null).get("summary");
        assertThat(after.get("rating").asDouble()).isEqualTo(4.5);
        assertThat(after.get("reviewCount").asInt()).isEqualTo(2);

        JsonNode list = get("/api/v1/cooks/" + cook.cookId() + "/reviews", null);
        assertThat(list.get("totalElements").asInt()).isEqualTo(2);
        assertThat(list.toString()).doesNotContain(c.email());
    }

    @Test
    void favorites() {
        Acct cook = approvedCook();
        Acct c = registerCustomer();
        post("/api/v1/cooks/" + cook.cookId() + "/favorite", null, null, 401);
        post("/api/v1/cooks/" + cook.cookId() + "/favorite", c.token(), null, 204);
        post("/api/v1/cooks/" + cook.cookId() + "/favorite", c.token(), null, 204); // idempotent
        assertThat(get("/api/v1/favorites", c.token()).size()).isEqualTo(1);
        assertThat(get("/api/v1/cooks/" + cook.cookId(), c.token()).get("favorite").asBoolean()).isTrue();
        delete("/api/v1/cooks/" + cook.cookId() + "/favorite", c.token(), 204);
        assertThat(get("/api/v1/favorites", c.token())).isEmpty();
        post("/api/v1/cooks/" + UUID.randomUUID() + "/favorite", c.token(), null, 404);
    }
}
