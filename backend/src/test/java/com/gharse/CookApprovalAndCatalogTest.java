package com.gharse;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CookApprovalAndCatalogTest extends IntegrationTestBase {

    // ------------------------------------------------------------ approval

    @Test
    void pendingCookCannotPublishFood() {
        Acct cook = registerCook("Pending " + unique());
        JsonNode err = post("/api/v1/cook/dishes", cook.token(), Map.of("name", "Poha", "category", "MEAL", "price", 50), 403);
        assertThat(err.get("code").asText()).isEqualTo("COOK_NOT_APPROVED");
        post("/api/v1/cook/batches", cook.token(), batchBody(UUID.randomUUID(), 5, tomorrow(), Instant.now().plus(5, ChronoUnit.HOURS)), 403);
    }

    @Test
    void onlyAdminsCanApproveOrReject_andACookCannotApproveThemselves() {
        Acct cook = registerCook("Self " + unique());
        Acct customer = registerCustomer();
        post("/api/v1/admin/cooks/" + cook.cookId() + "/approve", cook.token(), null, 403);
        post("/api/v1/admin/cooks/" + cook.cookId() + "/approve", customer.token(), null, 403);
        post("/api/v1/admin/cooks/" + cook.cookId() + "/reject", cook.token(), Map.of("reason", "x"), 403);
        assertThat(get("/api/v1/cook/profile", cook.token()).get("approvalStatus").asText()).isEqualTo("PENDING");
    }

    @Test
    void adminApprovalPublishesTheCookAndNotifiesThem() {
        Acct admin = admin();
        String name = "Meena " + unique();
        Acct cook = registerCook(name);
        // hidden from public until approved
        get("/api/v1/cooks/" + cook.cookId(), null, 404);
        assertThat(searchNames(name)).isEmpty();

        JsonNode pending = get("/api/v1/admin/cooks/pending?size=100", admin.token());
        assertThat(ids(pending.get("content"))).contains(cook.cookId().toString());

        JsonNode approved = post("/api/v1/admin/cooks/" + cook.cookId() + "/approve", admin.token(), null);
        assertThat(approved.get("approvalStatus").asText()).isEqualTo("APPROVED");
        assertThat(get("/api/v1/cooks/" + cook.cookId(), null).path("summary").get("name").asText()).isEqualTo(name);
        assertThat(searchNames(name)).contains(name);

        JsonNode notes = get("/api/v1/notifications", cook.token());
        assertThat(notes.get("items").toString()).contains("COOK_APPROVED");
        createDish(cook, "Puran Poli", 120);
    }

    @Test
    void rejectedCookStaysHiddenAndCanResubmit() {
        Acct admin = admin();
        Acct cook = registerCook("Rejected " + unique());
        post("/api/v1/admin/cooks/" + cook.cookId() + "/reject", admin.token(), Map.of("reason", "Please add kitchen photos"));
        JsonNode profile = get("/api/v1/cook/profile", cook.token());
        assertThat(profile.get("approvalStatus").asText()).isEqualTo("REJECTED");
        assertThat(profile.get("rejectionReason").asText()).contains("kitchen photos");
        post("/api/v1/admin/cooks/" + cook.cookId() + "/reject", admin.token(), Map.of("reason", ""), 400);
        get("/api/v1/cooks/" + cook.cookId(), null, 404);

        JsonNode updated = put("/api/v1/cook/profile", cook.token(), Map.of("kitchenPhotos", List.of("https://example.test/k.jpg")));
        assertThat(updated.get("approvalStatus").asText()).isEqualTo("PENDING");
    }

    @Test
    void cookCanCompleteProfileWithStoryCuisinesAndKitchen() {
        Acct cook = registerCook("Asha " + unique());
        JsonNode cuisines = get("/api/v1/cuisines", null);
        String maharashtrian = null;
        for (JsonNode c : cuisines) if (c.get("name").asText().equals("Maharashtrian")) maharashtrian = c.get("id").asText();
        JsonNode p = put("/api/v1/cook/profile", cook.token(), Map.ofEntries(
                Map.entry("headline", "Authentic Kolhapuri Food"), Map.entry("bio", "Cooking has been part of my family for 25 years."),
                Map.entry("yearsOfCooking", 25), Map.entry("locality", "Kothrud"), Map.entry("signatureDishes", "Puran Poli, Misal"),
                Map.entry("cuisineIds", List.of(maharashtrian)), Map.entry("kitchenAddress", "Plot 4, Kothrud, Pune"),
                Map.entry("kitchenDescription", "Clean home kitchen"), Map.entry("latitude", 18.5074), Map.entry("longitude", 73.8077)));
        assertThat(p.get("headline").asText()).isEqualTo("Authentic Kolhapuri Food");
        assertThat(p.get("cuisines").get(0).get("name").asText()).isEqualTo("Maharashtrian");
        assertThat(p.path("kitchen").get("address").asText()).contains("Kothrud");
        put("/api/v1/cook/profile", cook.token(), Map.of("cuisineIds", List.of(UUID.randomUUID().toString())), 400);
        // FSSAI document is optional unless admin configures it
        post("/api/v1/cook/documents", cook.token(), Map.of("documentType", "FSSAI", "documentNumber", "12345678901234"), 201);
        assertThat(get("/api/v1/cook/profile", cook.token()).get("documents").size()).isEqualTo(1);
    }

    // ------------------------------------------------------------ dishes

    @Test
    void cookCanOnlyModifyOwnDishes_andDeactivatedDishesDisappear() {
        Acct a = approvedCook();
        Acct b = approvedCook();
        UUID dish = createDish(a, "Misal", 90);
        put("/api/v1/cook/dishes/" + dish, b.token(), Map.of("name", "Hacked", "category", "MEAL", "price", 1), 403);
        delete("/api/v1/cook/dishes/" + dish, b.token(), 403);

        JsonNode updated = put("/api/v1/cook/dishes/" + dish, a.token(), Map.of("name", "Misal Pav", "category", "SNACK", "price", 95.5,
                "vegetarian", true, "allergens", "gluten"));
        assertThat(updated.get("name").asText()).isEqualTo("Misal Pav");
        assertThat(get("/api/v1/dishes/" + dish, null).get("price").asDouble()).isEqualTo(95.5);
        assertThat(get("/api/v1/cooks/" + a.cookId() + "/dishes", null).size()).isEqualTo(1);

        delete("/api/v1/cook/dishes/" + dish, a.token(), 204);
        assertThat(get("/api/v1/cooks/" + a.cookId() + "/dishes", null).size()).isZero();
        get("/api/v1/dishes/" + dish, null, 404);
        // cook still sees the inactive dish in their own list
        assertThat(get("/api/v1/cook/dishes", a.token()).get(0).get("active").asBoolean()).isFalse();
    }

    @Test
    void deactivatedDishIsHiddenFromPublicBatchListings() {
        Acct cook = approvedCook("Batch cook " + unique());
        UUID dish = createDish(cook, "Hidden special", 90);
        UUID batch = createBatch(cook, dish, 10);

        assertThat(get("/api/v1/cooks/" + cook.cookId() + "/batches", null).size()).isEqualTo(1);
        assertThat(get("/api/v1/batches?limit=50", null).toString()).contains("Hidden special");

        delete("/api/v1/cook/dishes/" + dish, cook.token(), 204);

        assertThat(get("/api/v1/cooks/" + cook.cookId() + "/batches", null)).isEmpty();
        assertThat(get("/api/v1/batches?limit=50", null).toString()).doesNotContain("Hidden special");
        assertThat(get("/api/v1/cook/batches", cook.token()).toString()).contains(batch.toString(), "Hidden special");
    }

    @Test
    void dishValidationRejectsNegativePrice() {
        Acct a = approvedCook();
        JsonNode err = post("/api/v1/cook/dishes", a.token(), Map.of("name", "Free?", "category", "MEAL", "price", -5), 400);
        assertThat(err.get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    // ------------------------------------------------------------ batches

    @Test
    void batchCreationRules() {
        Acct a = approvedCook();
        Acct b = approvedCook();
        UUID dish = createDish(a, "Puran Poli", 120);
        UUID otherDish = createDish(b, "Biryani", 200);
        Instant cutoff = Instant.now().plus(5, ChronoUnit.HOURS);

        UUID batch = createBatch(a, dish, 30);
        JsonNode pub = batchOf(a.cookId(), batch);
        assertThat(pub.get("capacity").asInt()).isEqualTo(30);
        assertThat(pub.get("remainingQuantity").asInt()).isEqualTo(30);
        assertThat(pub.get("soldOut").asBoolean()).isFalse();
        assertThat(pub.get("dishes").get(0).get("price").asDouble()).isEqualTo(120.0);

        post("/api/v1/cook/batches", a.token(), batchBody(otherDish, 5, tomorrow(), cutoff), 403); // someone else's dish
        post("/api/v1/cook/batches", a.token(), batchBody(dish, 0, tomorrow(), cutoff), 400);      // capacity > 0
        post("/api/v1/cook/batches", a.token(), batchBody(dish, 5, tomorrow().minusDays(5), cutoff), 400); // past date

        Map<String, Object> badWindow = new java.util.HashMap<>(batchBody(dish, 5, tomorrow(), cutoff));
        badWindow.put("pickupStart", "15:00");
        badWindow.put("pickupEnd", "14:00");
        assertThat(post("/api/v1/cook/batches", a.token(), badWindow, 400).get("code").asText()).isEqualTo("BATCH_WINDOW_INVALID");

        Map<String, Object> lateCutoff = batchBody(dish, 5, tomorrow(), Instant.now().plus(5, ChronoUnit.DAYS));
        assertThat(post("/api/v1/cook/batches", a.token(), lateCutoff, 400).get("code").asText()).isEqualTo("BATCH_CUTOFF_INVALID");

        // only the owner can edit; edits apply
        Map<String, Object> edit = new java.util.HashMap<>(batchBody(dish, 40, tomorrow(), cutoff));
        put("/api/v1/cook/batches/" + batch, b.token(), edit, 403);
        assertThat(put("/api/v1/cook/batches/" + batch, a.token(), edit).get("capacity").asInt()).isEqualTo(40);
        assertThat(get("/api/v1/cook/batches", a.token()).size()).isEqualTo(1);
    }

    // ------------------------------------------------------------ search & filter

    @Test
    void searchAndFilterCooks() {
        String tag = unique();
        Acct meena = approvedCook("Meena" + tag);
        Acct asha = approvedCook("Asha" + tag);
        put("/api/v1/cook/profile", meena.token(), Map.of("locality", "Kothrud" + tag, "headline", "Kolhapuri " + tag));
        put("/api/v1/cook/profile", asha.token(), Map.of("locality", "Baner" + tag));
        UUID poli = createDish(meena, "Puran Poli " + tag, 120);
        post("/api/v1/cook/dishes", asha.token(), Map.of("name", "Chicken Biryani " + tag, "category", "MEAL", "price", 250, "vegetarian", false), 201);

        assertThat(searchNames("Meena" + tag)).containsExactly("Meena" + tag);          // by cook name
        assertThat(searchNames("Puran Poli " + tag)).containsExactly("Meena" + tag);    // by dish name
        assertThat(searchNames("Baner" + tag)).containsExactly("Asha" + tag);           // by locality
        assertThat(searchNames("Kolhapuri " + tag)).containsExactly("Meena" + tag);     // by speciality

        // vegetarian filter excludes a cook with only non-veg food
        assertThat(names(get("/api/v1/cooks?vegetarian=true&q=" + tag + "&size=50", null))).containsExactly("Meena" + tag);
        assertThat(names(get("/api/v1/cooks?category=SNACK&q=" + tag, null))).isEmpty();
        assertThat(names(get("/api/v1/cooks?minRating=4&q=" + tag, null))).isEmpty(); // new cooks have no rating

        // available today
        assertThat(names(get("/api/v1/cooks?availableToday=true&q=" + tag, null))).isEmpty();
        post("/api/v1/cook/batches", meena.token(), batchBody(poli, 10, java.time.LocalDate.now(ZONE), Instant.now().plus(30, ChronoUnit.MINUTES)), 201);
        assertThat(names(get("/api/v1/cooks?availableToday=true&q=" + tag, null))).containsExactly("Meena" + tag);
        assertThat(get("/api/v1/cooks?q=Meena" + tag, null).get("content").get(0).get("availableToday").asBoolean()).isTrue();

        // distance ordering when coordinates are supplied
        put("/api/v1/cook/profile", meena.token(), Map.of("latitude", 18.5074, "longitude", 73.8077));
        put("/api/v1/cook/profile", asha.token(), Map.of("latitude", 18.5590, "longitude", 73.7868));
        JsonNode near = get("/api/v1/cooks?q=" + tag + "&lat=18.5074&lng=73.8077", null).get("content");
        assertThat(names(near)).containsExactly("Meena" + tag, "Asha" + tag);
        assertThat(near.get(0).get("distanceKm").asDouble()).isLessThan(0.2);
        assertThat(near.get(1).get("distanceKm").asDouble()).isBetween(4.0, 9.0);
    }

    @Test
    void searchTreatsWildcardsAsPlainText() {
        assertThat(get("/api/v1/cooks?q=%25", null).get("content")).isEmpty();
        get("/api/v1/cooks?q=' OR 1=1 --", null);
    }

    // ------------------------------------------------------------ helpers

    private List<String> searchNames(String q) {
        return names(get("/api/v1/cooks?size=50&q=" + q, null)); // MockMvc encodes the URI itself
    }

    private List<String> names(JsonNode pageOrList) {
        JsonNode list = pageOrList.has("content") ? pageOrList.get("content") : pageOrList;
        List<String> out = new ArrayList<>();
        list.forEach(n -> out.add(n.has("name") ? n.get("name").asText() : n.path("summary").get("name").asText()));
        return out;
    }

    private List<String> ids(JsonNode list) {
        List<String> out = new ArrayList<>();
        list.forEach(n -> out.add(n.get("id").asText()));
        return out;
    }
}
