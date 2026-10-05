package com.gharse;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartAndOrderTest extends IntegrationTestBase {

    private record Setup(Acct cook, UUID dish, UUID batch) {}

    private Setup setup() {
        Acct cook = approvedCook();
        UUID dish = createDish(cook, "Puran Poli", 120);
        return new Setup(cook, dish, createBatch(cook, dish, 30));
    }

    // ------------------------------------------------------------ cart

    @Test
    void cartAddsMergesUpdatesAndRemoves() {
        Setup s = setup();
        Acct c = registerCustomer();
        addToCart(c, s.dish(), s.batch(), 2, 200);
        JsonNode cart = addToCart(c, s.dish(), s.batch(), 1, 200);
        assertThat(cart.get("items").size()).isEqualTo(1);
        assertThat(cart.get("itemCount").asInt()).isEqualTo(3);
        assertThat(cart.get("subtotal").asDouble()).isEqualTo(360.0);
        String itemId = cart.get("items").get(0).get("id").asText();
        assertThat(put("/api/v1/cart/items/" + itemId, c.token(), Map.of("quantity", 4)).get("subtotal").asDouble()).isEqualTo(480.0);
        put("/api/v1/cart/items/" + itemId, c.token(), Map.of("quantity", 0), 400);
        assertThat(delete("/api/v1/cart/items/" + itemId, c.token(), 200).get("items")).isEmpty();
    }

    @Test
    void cartHoldsOneCookOnly() {
        Setup a = setup();
        Setup b = setup();
        Acct c = registerCustomer();
        addToCart(c, a.dish(), a.batch(), 1, 200);
        JsonNode err = addToCart(c, b.dish(), b.batch(), 1, 409);
        assertThat(err.get("code").asText()).isEqualTo("CART_OTHER_COOK");
        assertThat(err.get("message").asText()).isEqualTo("Your cart contains items from another Home Cook.");
        delete("/api/v1/cart", c.token(), 204);
        assertThat(addToCart(c, b.dish(), b.batch(), 1, 200).get("cookId").asText()).isEqualTo(b.cook().cookId().toString());
    }

    @Test
    void inactiveDishCannotBeAddedOrOrdered() {
        Setup s = setup();
        Acct c = registerCustomer();
        addToCart(c, s.dish(), s.batch(), 1, 200);
        delete("/api/v1/cook/dishes/" + s.dish(), s.cook().token(), 204);
        assertThat(checkout(c, "PICKUP", 400).get("code").asText()).isEqualTo("DISH_INACTIVE");
        assertThat(addToCart(registerCustomer(), s.dish(), s.batch(), 1, 400).get("code").asText()).isEqualTo("DISH_INACTIVE");
    }

    @Test
    void cartsArePrivateAndEmptyCartCannotCheckOut() {
        Acct c = registerCustomer();
        assertThat(get("/api/v1/cart", c.token()).get("items")).isEmpty();
        assertThat(checkout(c, "PICKUP", 400).get("code").asText()).isEqualTo("CART_EMPTY");
        Setup s = setup();
        JsonNode cart = addToCart(c, s.dish(), s.batch(), 1, 200);
        delete("/api/v1/cart/items/" + cart.get("items").get(0).get("id").asText(), registerCustomer().token(), 404);
    }

    // ------------------------------------------------------------ order creation

    @Test
    void checkoutCreatesAPaidOrderAndEmptiesTheCart() {
        Setup s = setup();
        Acct c = registerCustomer();
        JsonNode o = placeOrder(c, s.dish(), s.batch(), 2);
        assertThat(o.get("orderNumber").asText()).matches("GS\\d+");
        assertThat(o.get("orderStatus").asText()).isEqualTo("CREATED");
        assertThat(o.get("paymentStatus").asText()).isEqualTo("PAID");
        assertThat(o.get("totalAmount").asDouble()).isEqualTo(240.0);
        assertThat(o.get("items").get(0).get("dishName").asText()).isEqualTo("Puran Poli");
        assertThat(get("/api/v1/cart", c.token()).get("items")).isEmpty();
        assertThat(get("/api/v1/orders", c.token()).get("content").size()).isEqualTo(1);
        // cook was notified
        assertThat(get("/api/v1/notifications", s.cook().token()).toString()).contains("ORDER_PLACED");
    }

    @Test
    void deliveryNeedsAnAddressAndAddsAFee() {
        Setup s = setup();
        Acct c = registerCustomer();
        addToCart(c, s.dish(), s.batch(), 1, 200);
        post("/api/v1/orders", c.token(), Map.of("orderType", "COOK_DELIVERY"), 400);
        JsonNode o = checkout(c, "COOK_DELIVERY", 201);
        assertThat(o.get("deliveryFee").asDouble()).isEqualTo(30.0);
        assertThat(o.get("totalAmount").asDouble()).isEqualTo(150.0);
        addToCart(c, s.dish(), s.batch(), 1, 200);
        assertThat(checkout(c, "THIRD_PARTY_DELIVERY", 201).get("deliveryFee").asDouble()).isEqualTo(60.0);
    }

    @Test
    void historicalOrdersKeepTheirPriceWhenTheDishPriceChanges() {
        Setup s = setup();
        Acct c = registerCustomer();
        JsonNode o = placeOrder(c, s.dish(), s.batch(), 1);
        put("/api/v1/cook/dishes/" + s.dish(), s.cook().token(), Map.of("name", "Puran Poli Deluxe", "category", "SWEET", "price", 999));
        JsonNode again = get("/api/v1/orders/" + o.get("id").asText(), c.token());
        assertThat(again.get("items").get(0).get("unitPrice").asDouble()).isEqualTo(120.0);
        assertThat(again.get("items").get(0).get("dishName").asText()).isEqualTo("Puran Poli");
        assertThat(again.get("totalAmount").asDouble()).isEqualTo(120.0);
    }

    @Test
    void customersOnlySeeTheirOwnOrders_cooksOnlyTheirs() {
        Setup s = setup();
        Setup other = setup();
        Acct owner = registerCustomer();
        JsonNode o = placeOrder(owner, s.dish(), s.batch(), 1);
        String id = o.get("id").asText();
        get("/api/v1/orders/" + id, registerCustomer().token(), 404);
        post("/api/v1/orders/" + id + "/cancel", registerCustomer().token(), null, 404);
        get("/api/v1/cook/orders/" + id, other.cook().token(), 404);
        post("/api/v1/cook/orders/" + id + "/confirm", other.cook().token(), null, 404);
        assertThat(get("/api/v1/cook/orders", other.cook().token()).get("content")).isEmpty();
        assertThat(get("/api/v1/cook/orders", s.cook().token()).get("content").size()).isEqualTo(1);
        get("/api/v1/cook/orders/" + id, s.cook().token());
    }

    // ------------------------------------------------------------ lifecycle

    @Test
    void pickupOrderWalksTheWholeLifecycle() {
        Setup s = setup();
        Acct c = registerCustomer();
        String id = placeOrder(c, s.dish(), s.batch(), 1).get("id").asText();
        String base = "/api/v1/cook/orders/" + id;
        String t = s.cook().token();
        assertThat(post(base + "/prepare", t, null, 409).get("code").asText()).isEqualTo("INVALID_ORDER_TRANSITION");
        assertThat(post(base + "/confirm", t, null).get("orderStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(post(base + "/prepare", t, null).get("orderStatus").asText()).isEqualTo("PREPARING");
        assertThat(post(base + "/ready", t, null).get("orderStatus").asText()).isEqualTo("READY");
        assertThat(post(base + "/dispatch", t, null).get("orderStatus").asText()).isEqualTo("READY_FOR_PICKUP");
        assertThat(post(base + "/handover", t, null).get("orderStatus").asText()).isEqualTo("PICKED_UP");
        assertThat(post(base + "/complete", t, null).get("orderStatus").asText()).isEqualTo("COMPLETED");
        assertThat(get("/api/v1/orders/" + id, c.token()).get("orderStatus").asText()).isEqualTo("COMPLETED");
        assertThat(get("/api/v1/notifications", c.token()).toString()).contains("ORDER_CONFIRMED", "ORDER_READY", "ORDER_COMPLETED");
        // earnings
        assertThat(get("/api/v1/cook/earnings", t).get("totalEarnings").asDouble()).isEqualTo(120.0);
        assertThat(get("/api/v1/cook/dashboard", t).get("totalEarnings").asDouble()).isEqualTo(120.0);
    }

    @Test
    void completeFromReadyFastForwardsDeliveryOrdersAndBooksACourier() {
        Setup s = setup();
        Acct c = registerCustomer();
        addToCart(c, s.dish(), s.batch(), 1, 200);
        String id = checkout(c, "THIRD_PARTY_DELIVERY", 201).get("id").asText();
        String base = "/api/v1/cook/orders/" + id;
        String t = s.cook().token();
        post(base + "/complete", t, null, 409);                      // not READY yet
        post(base + "/confirm", t, null);
        post(base + "/prepare", t, null);
        post(base + "/ready", t, null);
        JsonNode out = post(base + "/dispatch", t, null);
        assertThat(out.get("orderStatus").asText()).isEqualTo("OUT_FOR_DELIVERY");
        assertThat(out.get("deliveryProvider").asText()).isEqualTo("MOCK");
        assertThat(out.get("trackingUrl").asText()).isNotBlank();
        assertThat(post(base + "/complete", t, null).get("orderStatus").asText()).isEqualTo("COMPLETED");
    }

    @Test
    void pickupOrdersCannotBeDispatchedForDelivery() {
        Setup s = setup();
        String id = placeOrder(registerCustomer(), s.dish(), s.batch(), 1).get("id").asText();
        String base = "/api/v1/cook/orders/" + id;
        post(base + "/confirm", s.cook().token(), null);
        post(base + "/prepare", s.cook().token(), null);
        post(base + "/ready", s.cook().token(), null);
        assertThat(post(base + "/dispatch", s.cook().token(), null).get("orderStatus").asText()).isEqualTo("READY_FOR_PICKUP");
    }

    @Test
    void cancellationRules() {
        Setup s = setup();
        Acct c = registerCustomer();
        String id = placeOrder(c, s.dish(), s.batch(), 1).get("id").asText();
        JsonNode cancelled = post("/api/v1/orders/" + id + "/cancel", c.token(), null);
        assertThat(cancelled.get("orderStatus").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("paymentStatus").asText()).isEqualTo("REFUNDED");
        // cancelled orders cannot move on
        post("/api/v1/cook/orders/" + id + "/confirm", s.cook().token(), null, 409);
        post("/api/v1/cook/orders/" + id + "/prepare", s.cook().token(), null, 409);
        post("/api/v1/orders/" + id + "/cancel", c.token(), null, 409);

        // confirmed and completed orders cannot be cancelled
        String id2 = placeOrder(c, s.dish(), s.batch(), 1).get("id").asText();
        post("/api/v1/cook/orders/" + id2 + "/confirm", s.cook().token(), null);
        assertThat(post("/api/v1/orders/" + id2 + "/cancel", c.token(), null, 409).get("code").asText()).isEqualTo("ORDER_NOT_CANCELLABLE");
        post("/api/v1/cook/orders/" + id2 + "/prepare", s.cook().token(), null);
        post("/api/v1/cook/orders/" + id2 + "/ready", s.cook().token(), null);
        post("/api/v1/cook/orders/" + id2 + "/complete", s.cook().token(), null);
        post("/api/v1/orders/" + id2 + "/cancel", c.token(), null, 409);
    }

    @Test
    void adminSeesOrdersUsersAndDashboardNumbers() {
        Setup s = setup();
        placeOrder(registerCustomer(), s.dish(), s.batch(), 1);
        Acct admin = admin();
        assertThat(get("/api/v1/admin/orders?size=100", admin.token()).get("content").size()).isGreaterThanOrEqualTo(1);
        assertThat(get("/api/v1/admin/users?role=COOK", admin.token()).get("content").size()).isGreaterThanOrEqualTo(1);
        JsonNode d = get("/api/v1/admin/dashboard", admin.token());
        assertThat(d.get("totalOrders").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(d.get("approvedCooks").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(get("/api/v1/admin/cooks/" + s.cook().cookId(), admin.token()).get("dishes").size()).isEqualTo(1);
    }
}
