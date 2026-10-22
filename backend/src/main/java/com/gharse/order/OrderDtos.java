package com.gharse.order;

import com.gharse.payment.PaymentStatus;
import com.gharse.payment.PaymentCheckout;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {}

    public record AddCartItemRequest(@NotNull UUID dishId, @NotNull UUID batchId, @Min(1) int quantity) {}

    public record UpdateCartItemRequest(@Min(1) int quantity) {}

    public record CartItemResponse(UUID id, UUID dishId, String dishName, String imageUrl, UUID batchId, String batchName,
                                   LocalDate availableDate, BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                                   int remaining, boolean orderingOpen) {}

    public record CartResponse(UUID id, UUID cookId, String cookName, List<CartItemResponse> items, BigDecimal subtotal,
                               int itemCount) {}

    public record CreateOrderRequest(@NotNull OrderType orderType, @Size(max = 500) String deliveryAddress) {}

    public record OrderItemResponse(UUID dishId, String dishName, UUID batchId, String batchName, LocalDate availableDate,
                                    LocalTime pickupStart, LocalTime pickupEnd, LocalTime deliveryStart,
                                    LocalTime deliveryEnd, BigDecimal unitPrice, int quantity, BigDecimal totalPrice) {}

    public record OrderResponse(UUID id, String orderNumber, UUID cookId, String cookName, String cookLocality,
                                UUID customerId, String customerName, String customerPhone,
                                List<OrderItemResponse> items, BigDecimal subtotal, BigDecimal deliveryFee,
                                BigDecimal totalAmount, OrderType orderType, PaymentStatus paymentStatus,
                                OrderStatus orderStatus, String deliveryAddress, String pickupAddress,
                                String deliveryProvider, String trackingUrl, boolean canCancel, boolean reviewed,
                                Instant createdAt, Instant updatedAt, PaymentCheckout paymentCheckout) {}

    public record VerifyPaymentRequest(@jakarta.validation.constraints.NotBlank String razorpayOrderId,
                                       @jakarta.validation.constraints.NotBlank String razorpayPaymentId,
                                       @jakarta.validation.constraints.NotBlank String razorpaySignature) {}
}
