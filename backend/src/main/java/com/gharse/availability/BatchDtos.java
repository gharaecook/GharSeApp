package com.gharse.availability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class BatchDtos {

    private BatchDtos() {}

    public record BatchDishRequest(@NotNull UUID dishId, @DecimalMin("0.00") BigDecimal price) {}

    public record BatchRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 2000) String description,
            @NotNull LocalDate availableDate,
            @NotNull Instant orderCutoff,
            @Min(1) int capacity,
            LocalTime pickupStart,
            LocalTime pickupEnd,
            LocalTime deliveryStart,
            LocalTime deliveryEnd,
            BatchStatus status,
            @NotEmpty @Valid List<BatchDishRequest> dishes) {}

    public record BatchDishResponse(UUID dishId, String name, String imageUrl, BigDecimal price, boolean vegetarian,
                                    String portionDescription) {}

    public record BatchResponse(UUID id, UUID cookId, String cookName, String cookLocality, String name, String description,
                                LocalDate availableDate, Instant orderCutoff, int capacity, int reservedQuantity,
                                int remainingQuantity, boolean soldOut, boolean orderingOpen, LocalTime pickupStart,
                                LocalTime pickupEnd, LocalTime deliveryStart, LocalTime deliveryEnd, BatchStatus status,
                                List<BatchDishResponse> dishes) {

        public static BatchResponse of(Batch b, Instant now) {
            var cook = b.getCook();
            return new BatchResponse(b.getId(), cook.getId(), cook.getUser().getName(), cook.getLocality(), b.getName(),
                    b.getDescription(), b.getAvailableDate(), b.getOrderCutoff(), b.getCapacity(), b.getReservedQuantity(),
                    b.remaining(), b.isSoldOut(), b.isOrderingOpen(now), b.getPickupStart(), b.getPickupEnd(),
                    b.getDeliveryStart(), b.getDeliveryEnd(), b.getStatus(),
                    b.getDishes().stream().map(i -> new BatchDishResponse(i.getDish().getId(), i.getDish().getName(),
                            i.getDish().getImageUrl(), i.getPrice(), i.getDish().isVegetarian(),
                            i.getDish().getPortionDescription())).toList());
        }
    }
}
