package com.gharse;

import static org.assertj.core.api.Assertions.assertThat;

import com.gharse.order.OrderStatus;
import com.gharse.order.OrderType;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void onlyCreatedOrdersCanBeCancelled() {
        for (OrderStatus s : OrderStatus.values()) {
            assertThat(s.canMoveTo(OrderStatus.CANCELLED)).as(s.name()).isEqualTo(s == OrderStatus.CREATED);
        }
    }

    @Test
    void terminalStatesHaveNoSuccessors() {
        assertThat(OrderStatus.COMPLETED.allowedNext()).isEmpty();
        assertThat(OrderStatus.CANCELLED.allowedNext()).isEmpty();
        assertThat(OrderStatus.CANCELLED.canMoveTo(OrderStatus.PREPARING)).isFalse();
    }

    @Test
    void happyPathsDifferByFulfilmentType() {
        assertThat(OrderStatus.READY.nextFor(OrderType.PICKUP)).isEqualTo(OrderStatus.READY_FOR_PICKUP);
        assertThat(OrderStatus.READY.nextFor(OrderType.COOK_DELIVERY)).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        assertThat(OrderStatus.PICKED_UP.nextFor(OrderType.PICKUP)).isEqualTo(OrderStatus.COMPLETED);
        assertThat(OrderStatus.CREATED.canMoveTo(OrderStatus.PREPARING)).isFalse();
    }
}
