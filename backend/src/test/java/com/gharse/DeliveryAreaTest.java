package com.gharse;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeliveryAreaTest extends IntegrationTestBase {

    @Test
    void adminCanManageNamedDeliveryAreas() {
        Acct admin = admin();
        Map<String, Object> place = Map.of(
                "placeId", unique(),
                "name", "Bavdhan",
                "formattedAddress", "Bavdhan, Pune, Maharashtra, India",
                "locality", "Bavdhan",
                "city", "Pune",
                "latitude", 18.5236,
                "longitude", 73.7812,
                "status", "NOT_STARTED");

        JsonNode area = post("/api/v1/admin/delivery-areas", admin.token(), place);
        assertThat(area.path("status").asText()).isEqualTo("NOT_STARTED");
        assertThat(get("/api/v1/admin/delivery-areas", admin.token()).toString()).contains(area.path("id").asText());
        put("/api/v1/admin/delivery-areas/" + area.path("id").asText() + "/status", admin.token(),
                Map.of("status", "DELIVERING"));
        assertThat(get("/api/v1/admin/delivery-areas", admin.token()).get(0).path("status").asText()).isEqualTo("DELIVERING");

        delete("/api/v1/admin/delivery-areas/" + area.path("id").asText(), admin.token(), 200);
        assertThat(get("/api/v1/admin/delivery-areas", admin.token()).size()).isZero();
    }

    @Test
    void deliveryAreaManagementIsAdminOnlyAndValidatesCoordinates() {
        Acct customer = registerCustomer();
        get("/api/v1/admin/delivery-areas", customer.token(), 403);

        Map<String, Object> invalid = Map.of(
                "placeId", unique(),
                "name", "Outside India",
                "formattedAddress", "Test",
                "locality", "Test",
                "city", "Test",
                "latitude", 91,
                "longitude", 0,
                "status", "DELIVERING");
        post("/api/v1/admin/delivery-areas", admin().token(), invalid, 400);
    }

    @Test
    void localitySearchMatchesBothLocalityAndCity() {
        approvedCook();
        JsonNode results = get("/api/v1/cooks?locality=Kothrud%20Pune", null);
        assertThat(results.path("content").toString()).contains("Kothrud");
    }
}
