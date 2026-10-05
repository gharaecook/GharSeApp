package com.gharse.review;

import com.gharse.common.ApiException;
import com.gharse.common.PageResponse;
import com.gharse.cook.Cook;
import com.gharse.cook.CookService;
import com.gharse.notification.NotificationService;
import com.gharse.notification.NotificationType;
import com.gharse.order.Order;
import com.gharse.order.OrderRepository;
import com.gharse.order.OrderStatus;
import com.gharse.review.ReviewDtos.ReviewRequest;
import com.gharse.review.ReviewDtos.ReviewResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final CookService cookService;
    private final NotificationService notifications;

    public ReviewService(ReviewRepository reviews, OrderRepository orders, CookService cookService,
                         NotificationService notifications) {
        this.reviews = reviews;
        this.orders = orders;
        this.cookService = cookService;
        this.notifications = notifications;
    }

    /** Only the customer of a COMPLETED order may review it, once. */
    public ReviewResponse create(UUID customerId, UUID orderId, ReviewRequest r) {
        Order order = orders.findByIdAndCustomerId(orderId, customerId).orElseThrow(() -> ApiException.notFound("Order"));
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw ApiException.conflict("ORDER_NOT_COMPLETED", "You can review an order once it is completed");
        }
        if (reviews.existsByOrderId(orderId)) {
            throw ApiException.conflict("REVIEW_ALREADY_EXISTS", "You have already reviewed this order");
        }
        Review review = new Review();
        review.setOrderId(orderId);
        review.setCustomer(order.getCustomer());
        review.setCook(order.getCook());
        review.setRating(r.rating());
        review.setTasteRating(r.tasteRating());
        review.setHygieneRating(r.hygieneRating());
        review.setPackagingRating(r.packagingRating());
        review.setReliabilityRating(r.reliabilityRating());
        review.setComment(r.comment() == null || r.comment().isBlank() ? null : r.comment().trim());
        review = reviews.saveAndFlush(review);

        Cook cook = order.getCook();
        Double avg = reviews.averageRating(cook.getId());
        cookService.applyRating(cook.getId(), BigDecimal.valueOf(avg == null ? 0 : avg).setScale(2, RoundingMode.HALF_UP),
                (int) reviews.countByCookId(cook.getId()));
        notifications.notify(cook.getUser().getId(), NotificationType.REVIEW_RECEIVED, "New review",
                "You received a " + r.rating() + "-star review for order " + order.getOrderNumber() + ".");
        return ReviewResponse.of(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> ofCook(UUID cookId, int page, int size) {
        cookService.getApproved(cookId);
        return PageResponse.of(reviews.findByCookId(cookId, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")))),
                ReviewResponse::of);
    }
}
