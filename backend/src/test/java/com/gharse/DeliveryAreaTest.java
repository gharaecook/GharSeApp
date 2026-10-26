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
                "areaType", "PLACE",
                "placeId", unique(),
                "name", "Bavdhan",
                "formattedAddress", "Bavdhan, Pune, Maharashtra, India",
                "locality", "Bavdhan",
                "city", "Pune",
                "latitude", 18.5236,
                "longitude", 73.7812,
                "status", "NOT_SUPPORTED");

        JsonNode area = post("/api/v1/admin/delivery-areas", admin.token(), place);
        assertThat(area.path("status").asText()).isEqualTo("NOT_SUPPORTED");
        assertThat(get("/api/v1/admin/delivery-areas", admin.token()).toString()).contains(area.path("id").asText());
        put("/api/v1/admin/delivery-areas/" + area.path("id").asText() + "/status", admin.token(),
                Map.of("status", "SUPPORTED"));
        assertThat(get("/api/v1/admin/delivery-areas", admin.token()).get(0).path("status").asText()).isEqualTo("SUPPORTED");

        delete("/api/v1/admin/delivery-areas/" + area.path("id").asText(), admin.token(), 200);
        assertThat(get("/api/v1/admin/delivery-areas", admin.token()).size()).isZero();
    }

    @Test
    void adminCanSaveAndRemoveDrawnDeliveryRegions() {
        Acct admin = admin();
        Map<String, Object> region = Map.of(
                "areaType", "REGION",
                "name", "West Pune",
                "status", "SUPPORTED",
                "boundary", java.util.List.of(
                        Map.of("latitude", 18.55, "longitude", 73.75),
                        Map.of("latitude", 18.50, "longitude", 73.72),
                        Map.of("latitude", 18.48, "longitude", 73.80)));

        JsonNode area = post("/api/v1/admin/delivery-areas", admin.token(), region);
        assertThat(area.path("areaType").asText()).isEqualTo("REGION");
        assertThat(area.path("boundary").size()).isEqualTo(3);
        assertThat(area.path("status").asText()).isEqualTo("SUPPORTED");
        delete("/api/v1/admin/delivery-areas/" + area.path("id").asText(), admin.token(), 200);
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
                "status", "SUPPORTED");
        post("/api/v1/admin/delivery-areas", admin().token(), invalid, 400);
    }

    @Test
    void localitySearchMatchesBothLocalityAndCity() {
        approvedCook();
        JsonNode results = get("/api/v1/cooks?locality=Kothrud%20Pune", null);
        assertThat(results.path("content").toString()).contains("Kothrud");
    }
}
