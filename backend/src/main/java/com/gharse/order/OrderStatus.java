package com.gharse.order;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order lifecycle:
 * CREATED -> CONFIRMED -> PREPARING -> READY -> (OUT_FOR_DELIVERY -> DELIVERED | READY_FOR_PICKUP -> PICKED_UP) -> COMPLETED,
 * and CREATED -> CANCELLED. Every other transition is invalid.
 */
public enum OrderStatus {
    CREATED, CONFIRMED, PREPARING, READY, OUT_FOR_DELIVERY, READY_FOR_PICKUP, DELIVERED, PICKED_UP, COMPLETED, CANCELLED;

    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case CREATED -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(PREPARING);
            case PREPARING -> EnumSet.of(READY);
            case READY -> EnumSet.of(OUT_FOR_DELIVERY, READY_FOR_PICKUP);
            case OUT_FOR_DELIVERY -> EnumSet.of(DELIVERED);
            case READY_FOR_PICKUP -> EnumSet.of(PICKED_UP);
            case DELIVERED, PICKED_UP -> EnumSet.of(COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canMoveTo(OrderStatus next) {
        return allowedNext().contains(next);
    }

    /** The next step on the happy path for the given fulfilment type, or null when terminal. */
    public OrderStatus nextFor(OrderType type) {
        return switch (this) {
            case CREATED -> CONFIRMED;
            case CONFIRMED -> PREPARING;
            case PREPARING -> READY;
            case READY -> type == OrderType.PICKUP ? READY_FOR_PICKUP : OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> DELIVERED;
            case READY_FOR_PICKUP -> PICKED_UP;
            case DELIVERED, PICKED_UP -> COMPLETED;
            case COMPLETED, CANCELLED -> null;
        };
    }
}
