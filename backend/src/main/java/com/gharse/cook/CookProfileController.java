package com.gharse.cook;

import com.gharse.auth.AuthUser;
import com.gharse.availability.BatchRepository;
import com.gharse.availability.BatchStatus;
import com.gharse.cook.CookDtos.CookDashboard;
import com.gharse.cook.CookDtos.CookProfileResponse;
import com.gharse.cook.CookDtos.DocumentRequest;
import com.gharse.cook.CookDtos.DocumentResponse;
import com.gharse.cook.CookDtos.EarningLine;
import com.gharse.cook.CookDtos.Earnings;
import com.gharse.cook.CookDtos.UpdateCookProfileRequest;
import com.gharse.order.Order;
import com.gharse.order.OrderRepository;
import com.gharse.order.OrderStatus;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The cook's own area: profile, documents, dashboard and earnings. */
@RestController
@RequestMapping("/api/v1/cook")
public class CookProfileController {

    private static final Set<OrderStatus> OPEN = EnumSet.complementOf(EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
    private static final Set<OrderStatus> PENDING_ACTION = EnumSet.of(OrderStatus.CREATED);

    private final CookService service;
    private final OrderRepository orders;
    private final BatchRepository batches;
    private final Clock clock;
    private final ZoneId zone;

    public CookProfileController(CookService service, OrderRepository orders, BatchRepository batches, Clock clock, ZoneId zone) {
        this.service = service;
        this.orders = orders;
        this.batches = batches;
        this.clock = clock;
        this.zone = zone;
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public CookProfileResponse profile() {
        return service.myProfile(AuthUser.current().id());
    }

    @PutMapping("/profile")
    public CookProfileResponse update(@Valid @RequestBody UpdateCookProfileRequest r) {
        return service.updateProfile(AuthUser.current().id(), r);
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse addDocument(@Valid @RequestBody DocumentRequest r) {
        return service.addDocument(AuthUser.current().id(), r);
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public CookDashboard dashboard() {
        Cook cook = service.requireCook(AuthUser.current().id());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        return new CookDashboard(cook.getApprovalStatus(),
                orders.countForDate(cook.getId(), today),
                orders.countAfter(cook.getId(), today),
                orders.countByCookIdAndOrderStatusIn(cook.getId(), PENDING_ACTION),
                batches.countByCookIdAndStatusAndAvailableDateGreaterThanEqual(cook.getId(), BatchStatus.OPEN, today),
                zero(orders.earnings(cook.getId(), OrderStatus.COMPLETED, Instant.EPOCH)),
                zero(orders.earnings(cook.getId(), OrderStatus.COMPLETED, monthStart)));
    }

    @GetMapping("/earnings")
    @Transactional(readOnly = true)
    public Earnings earnings() {
        Cook cook = service.requireCook(AuthUser.current().id());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        List<Order> recent = orders.findTop10ByCookIdAndOrderStatusOrderByUpdatedAtDesc(cook.getId(), OrderStatus.COMPLETED);
        return new Earnings(
                zero(orders.earnings(cook.getId(), OrderStatus.COMPLETED, Instant.EPOCH)),
                zero(orders.earningsIn(cook.getId(), OPEN)),
                zero(orders.earnings(cook.getId(), OrderStatus.COMPLETED, today.atStartOfDay(zone).toInstant())),
                zero(orders.earnings(cook.getId(), OrderStatus.COMPLETED, today.withDayOfMonth(1).atStartOfDay(zone).toInstant())),
                orders.countByCookIdAndOrderStatus(cook.getId(), OrderStatus.COMPLETED),
                recent.stream().map(o -> new EarningLine(o.getId(), o.getOrderNumber(),
                        o.getOrderType() == com.gharse.order.OrderType.COOK_DELIVERY ? o.getTotalAmount() : o.getSubtotal(), o.getUpdatedAt())).toList());
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
