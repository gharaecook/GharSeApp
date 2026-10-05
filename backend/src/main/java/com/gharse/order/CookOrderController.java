package com.gharse.order;

import com.gharse.auth.AuthUser;
import com.gharse.common.PageResponse;
import com.gharse.order.OrderDtos.OrderResponse;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Orders as seen and progressed by the cook who owns them. */
@RestController
@RequestMapping("/api/v1/cook/orders")
public class CookOrderController {

    private final OrderService service;

    public CookOrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<OrderResponse> list(@RequestParam(required = false) Set<OrderStatus> status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.cookOrders(AuthUser.current().id(), status, Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return service.cookOrder(AuthUser.current().id(), id);
    }

    @PostMapping("/{id}/confirm")
    public OrderResponse confirm(@PathVariable UUID id) {
        return service.step(AuthUser.current().id(), id, OrderStatus.CONFIRMED);
    }

    @PostMapping("/{id}/prepare")
    public OrderResponse prepare(@PathVariable UUID id) {
        return service.step(AuthUser.current().id(), id, OrderStatus.PREPARING);
    }

    @PostMapping("/{id}/ready")
    public OrderResponse ready(@PathVariable UUID id) {
        return service.step(AuthUser.current().id(), id, OrderStatus.READY);
    }

    /** READY -> READY_FOR_PICKUP (pickup orders) or OUT_FOR_DELIVERY (delivery orders). */
    @PostMapping("/{id}/dispatch")
    public OrderResponse dispatch(@PathVariable UUID id) {
        UUID user = AuthUser.current().id();
        OrderResponse o = service.cookOrder(user, id);
        return service.step(user, id, o.orderType() == OrderType.PICKUP ? OrderStatus.READY_FOR_PICKUP : OrderStatus.OUT_FOR_DELIVERY);
    }

    /** READY_FOR_PICKUP -> PICKED_UP or OUT_FOR_DELIVERY -> DELIVERED. */
    @PostMapping("/{id}/handover")
    public OrderResponse handover(@PathVariable UUID id) {
        UUID user = AuthUser.current().id();
        OrderResponse o = service.cookOrder(user, id);
        return service.step(user, id, o.orderType() == OrderType.PICKUP ? OrderStatus.PICKED_UP : OrderStatus.DELIVERED);
    }

    /** Completes a READY (or later) order, passing through the hand-over states. */
    @PostMapping("/{id}/complete")
    public OrderResponse complete(@PathVariable UUID id) {
        return service.complete(AuthUser.current().id(), id);
    }
}
