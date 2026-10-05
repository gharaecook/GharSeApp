package com.gharse.review;

import com.gharse.common.CreatedEntity;
import com.gharse.cook.Cook;
import com.gharse.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review")
@Getter
@Setter
@NoArgsConstructor
public class Review extends CreatedEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cook_id", nullable = false)
    private Cook cook;

    @Column(nullable = false)
    private int rating;

    @Column(name = "taste_rating")
    private Integer tasteRating;

    @Column(name = "hygiene_rating")
    private Integer hygieneRating;

    @Column(name = "packaging_rating")
    private Integer packagingRating;

    @Column(name = "reliability_rating")
    private Integer reliabilityRating;

    @Column(columnDefinition = "text")
    private String comment;
}
