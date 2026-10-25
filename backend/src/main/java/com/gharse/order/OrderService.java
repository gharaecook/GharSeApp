package com.gharse.order;

import com.gharse.availability.Batch;
import com.gharse.availability.BatchDishItem;
import com.gharse.availability.BatchRepository;
import com.gharse.common.ApiException;
import com.gharse.common.AppProperties;
import com.gharse.common.PageResponse;
import com.gharse.cook.Cook;
import com.gharse.cook.CookService;
import com.gharse.delivery.DeliveryOrder;
import com.gharse.delivery.DeliveryService;
import com.gharse.delivery.DeliveryStatus;
import com.gharse.kitchen.Kitchen;
import com.gharse.kitchen.KitchenRepository;
import com.gharse.notification.NotificationService;
import com.gharse.notification.NotificationType;
import com.gharse.order.OrderDtos.CreateOrderRequest;
import com.gharse.order.OrderDtos.OrderItemResponse;
import com.gharse.order.OrderDtos.OrderResponse;
import com.gharse.payment.PaymentService;
import com.gharse.payment.PaymentCheckout;
import com.gharse.payment.PaymentStatus;
import com.gharse.review.ReviewRepository;
import com.gharse.user.User;
import com.gharse.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order creation (atomic capacity reservation, price snapshot, payment) and the order lifecycle.
 * Customers and cooks only ever see their own orders; anything else is reported as 404 so ids cannot be probed.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orders;
    private final CartRepository carts;
    private final BatchRepository batches;
    private final KitchenRepository kitchens;
    private final UserRepository users;
    private final ReviewRepository reviews;
    private final PaymentService payments;
    private final DeliveryService delivery;
    private final NotificationService notifications;
    private final CookService cookService;
    private final EntityManager em;
    private final AppProperties props;
    private final Clock clock;

    public OrderService(OrderRepository orders, CartRepository carts, BatchRepository batches, KitchenRepository kitchens,
                        UserRepository users, ReviewRepository reviews, PaymentService payments, DeliveryService delivery,
                        NotificationService notifications, CookService cookService, EntityManager em, AppProperties props,
                        Clock clock) {
        this.orders = orders;
        this.carts = carts;
        this.batches = batches;
        this.kitchens = kitchens;
        this.users = users;
        this.reviews = reviews;
        this.payments = payments;
        this.delivery = delivery;
        this.notifications = notifications;
        this.cookService = cookService;
        this.em = em;
        this.props = props;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- customer

    public OrderResponse create(UUID customerId, CreateOrderRequest req) {
        Cart cart = carts.findByCustomerId(customerId).orElse(null);
        if (cart == null || cart.getItems().isEmpty() || cart.getCook() == null) {
            throw ApiException.badRequest("CART_EMPTY", "Your cart is empty");
        }
        Cook cook = cart.getCook();
        if (!cook.isApproved()) throw ApiException.conflict("COOK_UNAVAILABLE", "This Home Cook is not available right now");
        User customer = users.findById(customerId).orElseThrow(() -> ApiException.notFound("User"));
        Instant now = clock.instant();

        // 1. Validate every line against its dish/batch and group quantities per batch.
        Map<UUID, Integer> qtyByBatch = new LinkedHashMap<>();
        Map<UUID, Batch> batchById = new LinkedHashMap<>();
        for (CartItem ci : cart.getItems()) {
            Batch b = ci.getBatch();
            if (!ci.getDish().isActive()) {
                throw ApiException.badRequest("DISH_INACTIVE", ci.getDish().getName() + " is no longer available");
            }
            if (b.itemFor(ci.getDish().getId()) == null) {
                throw ApiException.badRequest("BATCH_MISMATCH", ci.getDish().getName() + " is no longer part of " + b.getName());
            }
            if (req.orderType() == OrderType.PICKUP && !b.supportsPickup()) {
                throw ApiException.badRequest("PICKUP_NOT_AVAILABLE", b.getName() + " is not available for pickup");
            }
            if (req.orderType().isDelivery() && !b.supportsDelivery()) {
                throw ApiException.badRequest("DELIVERY_NOT_AVAILABLE", b.getName() + " is not available for delivery");
            }
            qtyByBatch.merge(b.getId(), ci.getQuantity(), Integer::sum);
            batchById.put(b.getId(), b);
        }
        String address = req.deliveryAddress() != null && !req.deliveryAddress().isBlank()
                ? req.deliveryAddress().trim() : customer.deliveryAddress();
        if (req.orderType().isDelivery() && (address == null || address.isBlank())) {
            throw ApiException.badRequest("DELIVERY_ADDRESS_REQUIRED", "A delivery address is required");
        }

        // 2. Reserve capacity atomically. Any failure throws and rolls the whole transaction back.
        for (var e : qtyByBatch.entrySet()) {
            int updated = batches.reserve(e.getKey(), e.getValue(), now);
            if (updated == 0) {
                em.refresh(batchById.get(e.getKey()));
                throw reservationFailure(batchById.get(e.getKey()), e.getValue(), now);
            }
        }

        // 3. Build the order with price snapshots taken from the batch at this moment.
        Order order = new Order();
        order.setCustomer(customer);
        order.setCook(cook);
        order.setOrderType(req.orderType());
        order.setDeliveryAddress(req.orderType().isDelivery() ? address : null);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            BatchDishItem offered = ci.getBatch().itemFor(ci.getDish().getId());
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setDish(ci.getDish());
            oi.setBatch(ci.getBatch());
            oi.setDishNameSnapshot(ci.getDish().getName());
            oi.setUnitPrice(offered.getPrice());
            oi.setQuantity(ci.getQuantity());
            oi.setTotalPrice(offered.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            order.getItems().add(oi);
            subtotal = subtotal.add(oi.getTotalPrice());
        }
        order.setSubtotal(subtotal);
        order.setOrderNumber("GS" + orders.nextOrderNumber());
        order.setDeliveryFee(deliveryFee(order, cook));
        order.setTotalAmount(subtotal.add(order.getDeliveryFee()));
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orders.saveAndFlush(order);

        // 4. Create a payment through the configured provider.
        PaymentService.ChargeResult charge = payments.charge(order.getId(), order.getOrderNumber(), order.getTotalAmount());
        PaymentStatus paid = charge.status();
        if (paid != PaymentStatus.PAID && paid != PaymentStatus.AUTHORIZED && paid != PaymentStatus.PENDING) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", "Payment failed. You have not been charged.");
        }
        order.setPaymentStatus(paid);

        cart.getItems().clear();
        cart.setCook(null);

        if (paid == PaymentStatus.PAID || paid == PaymentStatus.AUTHORIZED) {
            notifyCookOfOrder(order);
        }
        return toResponse(order, true, charge.checkout());
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> myOrders(UUID customerId, int page, int size) {
        var result = orders.findByCustomerId(customerId, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
        return PageResponse.of(result, o -> toResponse(o, true));
    }

    @Transactional(readOnly = true)
    public OrderResponse myOrder(UUID customerId, UUID orderId) {
        Order order = orders.findByIdAndCustomerId(orderId, customerId).orElseThrow(() -> ApiException.notFound("Order"));
        return toResponse(order, true, payments.checkoutForOrder(orderId));
    }

    public OrderResponse verifyPayment(UUID customerId, UUID orderId, String razorpayOrderId,
                                       String razorpayPaymentId, String razorpaySignature) {
        Order order = orders.findByIdAndCustomerId(orderId, customerId).orElseThrow(() -> ApiException.notFound("Order"));
        if (order.getOrderStatus() != OrderStatus.CREATED || order.getPaymentStatus() == PaymentStatus.FAILED
                || order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "This order can no longer be paid");
        }
        boolean wasPaid = order.getPaymentStatus() == PaymentStatus.PAID;
        PaymentStatus status = payments.verify(orderId, razorpayOrderId, razorpayPaymentId, razorpaySignature);
        order.setPaymentStatus(status);
        if (!wasPaid) notifyCookOfOrder(order);
        return toResponse(order, true);
    }

    /** Only CREATED orders can be cancelled; the reserved portions go back to the batch and the payment is refunded. */
    public OrderResponse cancel(UUID customerId, UUID orderId) {
        Order o = orders.findByIdAndCustomerId(orderId, customerId).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getOrderStatus().canMoveTo(OrderStatus.CANCELLED)) {
            throw ApiException.conflict("ORDER_NOT_CANCELLABLE",
                    "An order can only be cancelled before the cook confirms it (current status: " + o.getOrderStatus() + ")");
        }
        Map<UUID, Integer> qtyByBatch = new LinkedHashMap<>();
        o.getItems().forEach(i -> qtyByBatch.merge(i.getBatch().getId(), i.getQuantity(), Integer::sum));
        qtyByBatch.forEach(batches::release);
        o.setOrderStatus(OrderStatus.CANCELLED);
        boolean notifiedCook = o.getPaymentStatus() == PaymentStatus.PAID || o.getPaymentStatus() == PaymentStatus.AUTHORIZED;
        if (o.getPaymentStatus() == PaymentStatus.PAID) {
            o.setPaymentStatus(payments.refund(o.getId(), o.getTotalAmount()));
        } else if (o.getPaymentStatus() == PaymentStatus.PENDING) {
            payments.cancelPending(o.getId());
            o.setPaymentStatus(PaymentStatus.FAILED);
        }
        if (notifiedCook) {
            notifications.notify(o.getCook().getUser().getId(), NotificationType.ORDER_CANCELLED,
                    "Order " + o.getOrderNumber() + " cancelled", "The customer cancelled this order.");
        }
        return toResponse(o, true);
    }

    // ---------------------------------------------------------------- cook

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> cookOrders(UUID userId, Set<OrderStatus> statuses, int page, int size) {
        Cook cook = cookService.requireCook(userId);
        var pr = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        var result = statuses == null || statuses.isEmpty() ? orders.findByCookId(cook.getId(), pr)
                : orders.findByCookIdAndOrderStatusIn(cook.getId(), statuses, pr);
        return PageResponse.of(result, o -> toResponse(o, true));
    }

    @Transactional(readOnly = true)
    public OrderResponse cookOrder(UUID userId, UUID orderId) {
        return toResponse(cookOwned(userId, orderId), true);
    }

    /** Moves the order exactly one step along the lifecycle to {@code target}. */
    public OrderResponse step(UUID userId, UUID orderId, OrderStatus target) {
        Order o = cookOwned(userId, orderId);
        transition(o, target);
        return toResponse(o, true);
    }

    /**
     * Completes an order that is READY or later, walking through the delivery/pickup hand-over states so the
     * lifecycle stays valid at every hop.
     */
    public OrderResponse complete(UUID userId, UUID orderId) {
        Order o = cookOwned(userId, orderId);
        if (o.getOrderStatus() == OrderStatus.COMPLETED) return toResponse(o, true);
        if (o.getOrderStatus().ordinal() < OrderStatus.READY.ordinal() || o.getOrderStatus() == OrderStatus.CANCELLED) {
            throw ApiException.conflict("INVALID_ORDER_TRANSITION",
                    "Order is " + o.getOrderStatus() + "; it must be READY before it can be completed");
        }
        while (o.getOrderStatus() != OrderStatus.COMPLETED) {
            transition(o, o.getOrderStatus().nextFor(o.getOrderType()));
        }
        return toResponse(o, true);
    }

    private Order cookOwned(UUID userId, UUID orderId) {
        Cook cook = cookService.requireCook(userId);
        return orders.findByIdAndCookId(orderId, cook.getId()).orElseThrow(() -> ApiException.notFound("Order"));
    }

    private void transition(Order o, OrderStatus target) {
        OrderStatus from = o.getOrderStatus();
        if (!from.canMoveTo(target) || target == OrderStatus.CANCELLED) {
            throw ApiException.conflict("INVALID_ORDER_TRANSITION", "Cannot move an order from " + from + " to " + target);
        }
        if (o.getPaymentStatus() != PaymentStatus.PAID && o.getPaymentStatus() != PaymentStatus.AUTHORIZED) {
            throw ApiException.conflict("PAYMENT_REQUIRED", "Payment must be completed before the cook can process this order");
        }
        if (target == OrderStatus.READY_FOR_PICKUP && o.getOrderType() != OrderType.PICKUP
                || target == OrderStatus.OUT_FOR_DELIVERY && o.getOrderType() == OrderType.PICKUP) {
            throw ApiException.conflict("INVALID_ORDER_TRANSITION", target + " does not apply to " + o.getOrderType() + " orders");
        }
        if (target == OrderStatus.OUT_FOR_DELIVERY && o.getOrderType() == OrderType.THIRD_PARTY_DELIVERY) {
            bookCourier(o);
        }
        if (target == OrderStatus.DELIVERED || target == OrderStatus.PICKED_UP) {
            delivery.markStatus(o.getId(), DeliveryStatus.DELIVERED);
        }
        o.setOrderStatus(target);

        UUID customerId = o.getCustomer().getId();
        String no = o.getOrderNumber();
        switch (target) {
            case CONFIRMED -> notifications.notify(customerId, NotificationType.ORDER_CONFIRMED, "Order confirmed",
                    o.getCook().getUser().getName() + " confirmed order " + no + ".");
            case READY -> notifications.notify(customerId, NotificationType.ORDER_READY, "Your food is ready",
                    "Order " + no + " is ready.");
            case COMPLETED -> notifications.notify(customerId, NotificationType.ORDER_COMPLETED, "Order completed",
                    "Order " + no + " is complete. How was it? Leave a review for " + o.getCook().getUser().getName() + ".");
            default -> notifications.notify(customerId, NotificationType.ORDER_STATUS_CHANGED, "Order update",
                    "Order " + no + " is now " + target.name().replace('_', ' ').toLowerCase() + ".");
        }
    }

    private void bookCourier(Order o) {
        Kitchen k = kitchens.findByCookId(o.getCook().getId()).orElse(null);
        String pickup = k != null && k.getAddress() != null ? k.getAddress() : o.getCook().getLocality();
        DeliveryOrder d = delivery.book(o.getId(), o.getOrderNumber(), pickup, o.getDeliveryAddress(), o.getDeliveryFee());
        o.setDeliveryProvider(d.getProvider());
        o.setDeliveryOrderId(d.getProviderOrderId());
    }

    private void notifyCookOfOrder(Order order) {
        notifications.notify(order.getCook().getUser().getId(), NotificationType.ORDER_PLACED,
                "New order " + order.getOrderNumber(),
                order.getCustomer().getName() + " ordered from you (" + order.getTotalAmount() + "). Confirm it to get started.");
    }

    // ---------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> all(OrderStatus status, int page, int size) {
        var pr = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        var result = status == null ? orders.findAll(pr) : orders.findByOrderStatus(status, pr);
        return PageResponse.of(result, o -> toResponse(o, true));
    }

    // ---------------------------------------------------------------- helpers

    private BigDecimal deliveryFee(Order order, Cook cook) {
        return switch (order.getOrderType()) {
            case PICKUP -> BigDecimal.ZERO;
            case COOK_DELIVERY -> props.order().cookDeliveryFee();
            case THIRD_PARTY_DELIVERY -> {
                Kitchen k = kitchens.findByCookId(cook.getId()).orElse(null);
                yield delivery.quote(order.getOrderNumber(), k == null ? null : k.getAddress(), order.getDeliveryAddress());
            }
        };
    }

    private ApiException reservationFailure(Batch b, int wanted, Instant now) {
        if (b.getStatus() != com.gharse.availability.BatchStatus.OPEN) {
            return ApiException.conflict("ORDERING_CLOSED", b.getName() + " is no longer accepting orders");
        }
        if (!now.isBefore(b.getOrderCutoff())) {
            return ApiException.conflict("ORDERING_CLOSED", "Pre-orders for " + b.getName() + " have closed");
        }
        if (b.isSoldOut()) return ApiException.conflict("BATCH_SOLD_OUT", b.getName() + " is sold out");
        return ApiException.conflict("BATCH_CAPACITY_EXCEEDED",
                "Only " + b.remaining() + " portion(s) left in " + b.getName() + ", you asked for " + wanted);
    }

    public OrderResponse toResponse(Order o, boolean includeContact) {
        return toResponse(o, includeContact, null);
    }

    private OrderResponse toResponse(Order o, boolean includeContact, PaymentCheckout checkout) {
        var items = o.getItems().stream().map(i -> {
            Batch b = i.getBatch();
            return new OrderItemResponse(i.getDish().getId(), i.getDishNameSnapshot(), b.getId(), b.getName(),
                    b.getAvailableDate(), b.getPickupStart(), b.getPickupEnd(), b.getDeliveryStart(), b.getDeliveryEnd(),
                    i.getUnitPrice(), i.getQuantity(), i.getTotalPrice());
        }).toList();
        String pickupAddress = null;
        if (o.getOrderType() == OrderType.PICKUP && o.getOrderStatus() != OrderStatus.CREATED
                && o.getOrderStatus() != OrderStatus.CANCELLED) {
            pickupAddress = kitchens.findByCookId(o.getCook().getId()).map(Kitchen::getAddress).orElse(null);
        }
        String tracking = delivery.forOrder(o.getId()).map(DeliveryOrder::getTrackingUrl).orElse(null);
        User c = o.getCustomer();
        return new OrderResponse(o.getId(), o.getOrderNumber(), o.getCook().getId(), o.getCook().getUser().getName(),
                o.getCook().getLocality(), c.getId(), c.getName(), includeContact ? c.getPhone() : null, items,
                o.getSubtotal(), o.getDeliveryFee(), o.getTotalAmount(), o.getOrderType(), o.getPaymentStatus(),
                o.getOrderStatus(), o.getDeliveryAddress(), pickupAddress, o.getDeliveryProvider(), tracking,
                o.getOrderStatus().canMoveTo(OrderStatus.CANCELLED), reviews.existsByOrderId(o.getId()),
                o.getCreatedAt(), o.getUpdatedAt(), checkout);
    }
}
