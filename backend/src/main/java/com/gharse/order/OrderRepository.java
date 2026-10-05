package com.gharse.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Order> findByCookId(UUID cookId, Pageable pageable);

    Page<Order> findByOrderStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByCookIdAndOrderStatusIn(UUID cookId, Collection<OrderStatus> statuses, Pageable pageable);

    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Order> findByIdAndCookId(UUID id, UUID cookId);

    long countByOrderStatus(OrderStatus status);

    @Query(value = "select nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();

    /** Earnings = item subtotal, plus the delivery fee when the cook delivered it themselves. */
    String EARNINGS = "sum(case when o.orderType = com.gharse.order.OrderType.COOK_DELIVERY then o.totalAmount else o.subtotal end)";

    @Query("select " + EARNINGS + " from Order o where o.cook.id = :cookId and o.orderStatus = :status and o.updatedAt >= :since")
    BigDecimal earnings(@Param("cookId") UUID cookId, @Param("status") OrderStatus status, @Param("since") Instant since);

    @Query("select " + EARNINGS + " from Order o where o.cook.id = :cookId and o.orderStatus in :statuses")
    BigDecimal earningsIn(@Param("cookId") UUID cookId, @Param("statuses") Collection<OrderStatus> statuses);

    long countByCookIdAndOrderStatus(UUID cookId, OrderStatus status);

    long countByCookIdAndOrderStatusIn(UUID cookId, Collection<OrderStatus> statuses);

    @Query("select count(distinct o.id) from Order o join o.items i where o.cook.id = :cookId "
            + "and i.batch.availableDate = :date and o.orderStatus <> com.gharse.order.OrderStatus.CANCELLED")
    long countForDate(@Param("cookId") UUID cookId, @Param("date") LocalDate date);

    @Query("select count(distinct o.id) from Order o join o.items i where o.cook.id = :cookId "
            + "and i.batch.availableDate > :date and o.orderStatus <> com.gharse.order.OrderStatus.CANCELLED")
    long countAfter(@Param("cookId") UUID cookId, @Param("date") LocalDate date);

    List<Order> findTop10ByCookIdAndOrderStatusOrderByUpdatedAtDesc(UUID cookId, OrderStatus status);

    @Query("select sum(o.totalAmount) from Order o where o.orderStatus = com.gharse.order.OrderStatus.COMPLETED")
    BigDecimal totalRevenue();
}
