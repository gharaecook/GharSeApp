package com.gharse.availability;

import com.gharse.availability.BatchDtos.BatchDishRequest;
import com.gharse.availability.BatchDtos.BatchRequest;
import com.gharse.availability.BatchDtos.BatchResponse;
import com.gharse.common.ApiException;
import com.gharse.cook.Cook;
import com.gharse.cook.CookService;
import com.gharse.menu.Dish;
import com.gharse.menu.DishRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BatchService {

    /** A batch counts as "limited" when at most this share of its capacity (or 5 portions) is left. */
    private static final double LIMITED_SHARE = 0.3;

    private final BatchRepository batches;
    private final DishRepository dishes;
    private final CookService cookService;
    private final Clock clock;
    private final ZoneId zone;

    public BatchService(BatchRepository batches, DishRepository dishes, CookService cookService, Clock clock, ZoneId zone) {
        this.batches = batches;
        this.dishes = dishes;
        this.cookService = cookService;
        this.clock = clock;
        this.zone = zone;
    }

    // ---------------------------------------------------------------- public

    public List<BatchResponse> upcomingOf(UUID cookId) {
        cookService.getApproved(cookId);
        Instant now = clock.instant();
        return batches.findUpcoming(cookId, BatchStatus.OPEN, today()).stream()
                .filter(b -> now.isBefore(b.getOrderCutoff()))
                .map(b -> BatchResponse.of(b, now)).toList();
    }

    /** Open batches across the marketplace. {@code today} restricts to today, {@code limited} to batches nearly sold out. */
    public List<BatchResponse> discover(boolean todayOnly, boolean limited, int limit) {
        Instant now = clock.instant();
        LocalDate today = today();
        return batches.findOpenForDiscovery(BatchStatus.OPEN, today, now).stream()
                .filter(b -> !b.isSoldOut())
                .filter(b -> !todayOnly || b.getAvailableDate().equals(today))
                .filter(b -> !limited || b.remaining() <= Math.max(5, Math.floor(b.getCapacity() * LIMITED_SHARE)))
                .limit(limit)
                .map(b -> BatchResponse.of(b, now)).toList();
    }

    // ---------------------------------------------------------------- cook

    public List<BatchResponse> mine(UUID userId) {
        Cook c = cookService.requireCook(userId);
        Instant now = clock.instant();
        return batches.findByCookIdOrderByAvailableDateDescCreatedAtDesc(c.getId()).stream()
                .map(b -> BatchResponse.of(b, now)).toList();
    }

    @Transactional
    public BatchResponse create(UUID userId, BatchRequest r) {
        Cook c = cookService.requireApprovedCook(userId);
        Batch b = new Batch();
        b.setCook(c);
        apply(b, r, c, true);
        return BatchResponse.of(batches.save(b), clock.instant());
    }

    @Transactional
    public BatchResponse update(UUID userId, UUID batchId, BatchRequest r) {
        Cook c = cookService.requireApprovedCook(userId);
        Batch b = batches.findById(batchId).orElseThrow(() -> ApiException.notFound("Batch"));
        if (!b.getCook().getId().equals(c.getId())) throw ApiException.forbidden("You can only modify your own batches");
        apply(b, r, c, false);
        return BatchResponse.of(batches.save(b), clock.instant());
    }

    private void apply(Batch b, BatchRequest r, Cook cook, boolean creating) {
        validateWindow(r.pickupStart(), r.pickupEnd(), "pickup");
        validateWindow(r.deliveryStart(), r.deliveryEnd(), "delivery");
        if (r.pickupStart() == null && r.deliveryStart() == null) {
            throw ApiException.badRequest("BATCH_NO_FULFILMENT", "Offer at least a pickup or a delivery window");
        }
        if (creating && r.availableDate().isBefore(today())) {
            throw ApiException.badRequest("BATCH_DATE_IN_PAST", "Batch date cannot be in the past");
        }
        if (r.orderCutoff().isAfter(r.availableDate().plusDays(1).atStartOfDay(zone).toInstant())) {
            throw ApiException.badRequest("BATCH_CUTOFF_INVALID", "Order cut-off must be on or before the batch day");
        }
        if (r.capacity() < b.getReservedQuantity()) {
            throw ApiException.badRequest("BATCH_CAPACITY_BELOW_RESERVED",
                    "Capacity cannot be lower than the " + b.getReservedQuantity() + " portions already reserved");
        }
        List<BatchDishItem> items = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (BatchDishRequest dr : r.dishes()) {
            if (!seen.add(dr.dishId())) throw ApiException.badRequest("BATCH_DUPLICATE_DISH", "A dish can only be listed once");
            Dish d = dishes.findById(dr.dishId()).orElseThrow(() -> ApiException.badRequest("DISH_NOT_FOUND", "Dish not found"));
            if (!d.getCook().getId().equals(cook.getId())) throw ApiException.forbidden("You can only add your own dishes");
            if (!d.isActive()) throw ApiException.badRequest("DISH_INACTIVE", d.getName() + " is inactive");
            items.add(new BatchDishItem(d, dr.price() != null ? dr.price() : d.getPrice()));
        }
        b.setName(r.name().trim());
        b.setDescription(r.description());
        b.setAvailableDate(r.availableDate());
        b.setOrderCutoff(r.orderCutoff());
        b.setCapacity(r.capacity());
        b.setPickupStart(r.pickupStart());
        b.setPickupEnd(r.pickupEnd());
        b.setDeliveryStart(r.deliveryStart());
        b.setDeliveryEnd(r.deliveryEnd());
        if (r.status() != null) b.setStatus(r.status());
        b.getDishes().clear();
        b.getDishes().addAll(items);
    }

    private static void validateWindow(LocalTime start, LocalTime end, String what) {
        if ((start == null) != (end == null)) {
            throw ApiException.badRequest("BATCH_WINDOW_INVALID", "Provide both start and end for " + what);
        }
        if (start != null && !start.isBefore(end)) {
            throw ApiException.badRequest("BATCH_WINDOW_INVALID", "The " + what + " window must end after it starts");
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }
}
