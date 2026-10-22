package com.gharse.order;

import com.gharse.auth.AuthUser;
import com.gharse.common.PageResponse;
import com.gharse.order.OrderDtos.CreateOrderRequest;
import com.gharse.order.OrderDtos.OrderResponse;
import com.gharse.order.OrderDtos.VerifyPaymentRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** Turns the current cart into an order and creates a payment with the configured provider. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest r) {
        return service.create(AuthUser.current().id(), r);
    }

    @PostMapping("/{id}/payment/verify")
    public OrderResponse verifyPayment(@PathVariable UUID id, @Valid @RequestBody VerifyPaymentRequest r) {
        return service.verifyPayment(AuthUser.current().id(), id, r.razorpayOrderId(),
                r.razorpayPaymentId(), r.razorpaySignature());
    }

    @GetMapping
    public PageResponse<OrderResponse> mine(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.myOrders(AuthUser.current().id(), Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return service.myOrder(AuthUser.current().id(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) {
        return service.cancel(AuthUser.current().id(), id);
    }
}
