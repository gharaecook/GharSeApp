package com.gharse;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** The critical business rule: a batch can never be oversold. */
class BatchCapacityTest extends IntegrationTestBase {

    private record Setup(Acct cook, UUID dish, UUID batch) {}

    private Setup setup(int capacity) {
        Acct cook = approvedCook();
        UUID dish = createDish(cook, "Puran Poli", 120);
        return new Setup(cook, dish, createBatch(cook, dish, capacity));
    }

    @Test
    void whenCapacityIsFullAnotherOrderFails() {
        Setup s = setup(10);
        Acct early = registerCustomer();
        Acct late = registerCustomer();
        addToCart(late, s.dish(), s.batch(), 1, 200);          // in the cart while there is still room

        placeOrder(early, s.dish(), s.batch(), 10);            // capacity 10, reserved 10
        JsonNode b = batchOf(s.cook().cookId(), s.batch());
        assertThat(b.get("reservedQuantity").asInt()).isEqualTo(10);
        assertThat(b.get("remainingQuantity").asInt()).isZero();
        assertThat(b.get("soldOut").asBoolean()).isTrue();      // "SOLD OUT"

        JsonNode err = checkout(late, "PICKUP", 409);
        assertThat(err.get("code").asText()).isEqualTo("BATCH_SOLD_OUT");
        addToCart(registerCustomer(), s.dish(), s.batch(), 1, 409); // cannot even add to a cart
        assertThat(batchOf(s.cook().cookId(), s.batch()).get("reservedQuantity").asInt()).isEqualTo(10);
    }

    @Test
    void orderingMoreThanTheRemainingPortionsFails() {
        Setup s = setup(10);
        Acct first = registerCustomer();
        Acct second = registerCustomer();
        addToCart(second, s.dish(), s.batch(), 3, 200);         // wants 3 while 10 remain

        placeOrder(first, s.dish(), s.batch(), 8);              // capacity 10, reserved 8
        JsonNode err = checkout(second, "PICKUP", 409);         // 3 > 2 remaining
        assertThat(err.get("code").asText()).isEqualTo("BATCH_CAPACITY_EXCEEDED");
        assertThat(batchOf(s.cook().cookId(), s.batch()).get("reservedQuantity").asInt()).isEqualTo(8); // unchanged

        // exactly the remaining 2 is fine
        addToCart(second, s.dish(), s.batch(), 0 + 0, 400);     // quantity must be > 0
        post("/api/v1/cart/items", second.token(), java.util.Map.of("dishId", s.dish().toString(), "batchId", s.batch().toString(), "quantity", 2), 409);
        JsonNode ok = put("/api/v1/cart/items/" + get("/api/v1/cart", second.token()).get("items").get(0).get("id").asText(),
                second.token(), java.util.Map.of("quantity", 2));
        assertThat(ok.get("itemCount").asInt()).isEqualTo(2);
        checkout(second, "PICKUP", 201);
        assertThat(batchOf(s.cook().cookId(), s.batch()).get("soldOut").asBoolean()).isTrue();
    }

    @Test
    void failedCheckoutDoesNotKeepAnyReservation() {
        Acct cook = approvedCook();
        UUID dish = createDish(cook, "Thali", 150);
        UUID plentiful = createBatch(cook, dish, 20);
        UUID scarce = createBatch(cook, dish, 2);
        Acct blocker = registerCustomer();
        Acct customer = registerCustomer();
        addToCart(customer, dish, plentiful, 5, 200);
        addToCart(customer, dish, scarce, 2, 200);
        placeOrder(blocker, dish, scarce, 2);                   // scarce batch gone before checkout

        checkout(customer, "PICKUP", 409);
        // the plentiful batch must not have been charged for the failed order
        assertThat(batchOf(cook.cookId(), plentiful).get("reservedQuantity").asInt()).isZero();
    }

    @Test
    void cancellingReleasesThePortions() {
        Setup s = setup(10);
        Acct c = registerCustomer();
        JsonNode order = placeOrder(c, s.dish(), s.batch(), 10);
        assertThat(batchOf(s.cook().cookId(), s.batch()).get("soldOut").asBoolean()).isTrue();
        post("/api/v1/orders/" + order.get("id").asText() + "/cancel", c.token(), null);
        JsonNode b = batchOf(s.cook().cookId(), s.batch());
        assertThat(b.get("reservedQuantity").asInt()).isZero();
        assertThat(b.get("soldOut").asBoolean()).isFalse();
    }

    @Test
    void closedBatchOrBatchPastCutoffAcceptsNoOrders() {
        Setup s = setup(10);
        Acct c = registerCustomer();
        addToCart(c, s.dish(), s.batch(), 1, 200);
        var closed = new java.util.HashMap<>(batchBody(s.dish(), 10, tomorrow(), java.time.Instant.now().plus(5, java.time.temporal.ChronoUnit.HOURS)));
        closed.put("status", "CLOSED");
        put("/api/v1/cook/batches/" + s.batch(), s.cook().token(), closed);
        assertThat(checkout(c, "PICKUP", 409).get("code").asText()).isEqualTo("ORDERING_CLOSED");
        addToCart(registerCustomer(), s.dish(), s.batch(), 1, 409);
    }

    @Test
    void concurrentOrdersNeverOversell() throws Exception {
        Setup s = setup(10);
        int buyers = 8;                                         // each wants 3 => at most 3 can succeed
        List<Acct> customers = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            Acct c = registerCustomer();
            addToCart(c, s.dish(), s.batch(), 3, 200);
            customers.add(c);
        }
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        List<Callable<Integer>> tasks = new ArrayList<>();
        java.util.concurrent.CountDownLatch go = new java.util.concurrent.CountDownLatch(1);
        for (Acct c : customers) {
            tasks.add(() -> {
                go.await();
                return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/orders")
                                .header("Authorization", "Bearer " + c.token()).contentType("application/json")
                                .content("{\"orderType\":\"PICKUP\"}"))
                        .andReturn().getResponse().getStatus();
            });
        }
        List<Future<Integer>> results = new ArrayList<>();
        for (Callable<Integer> t : tasks) results.add(pool.submit(t));
        go.countDown();
        int created = 0;
        for (Future<Integer> f : results) if (f.get() == 201) created++;
        pool.shutdown();

        assertThat(created).isEqualTo(3);
        JsonNode b = batchOf(s.cook().cookId(), s.batch());
        assertThat(b.get("reservedQuantity").asInt()).isEqualTo(9);
        assertThat(b.get("reservedQuantity").asInt()).isLessThanOrEqualTo(b.get("capacity").asInt());
    }

    @Test
    void capacityCannotBeLoweredBelowWhatIsReserved() {
        Setup s = setup(10);
        placeOrder(registerCustomer(), s.dish(), s.batch(), 6);
        var body = batchBody(s.dish(), 5, tomorrow(), java.time.Instant.now().plus(5, java.time.temporal.ChronoUnit.HOURS));
        assertThat(put("/api/v1/cook/batches/" + s.batch(), s.cook().token(), body, 400).get("code").asText())
                .isEqualTo("BATCH_CAPACITY_BELOW_RESERVED");
    }
}
