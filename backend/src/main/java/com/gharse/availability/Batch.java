package com.gharse.availability;

import com.gharse.common.BaseEntity;
import com.gharse.cook.Cook;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicUpdate;

/**
 * A limited run of food a cook prepares for a given day, e.g. "Sunday Special: 30 portions of Puran Poli".
 * {@code capacity} is the total number of portions across the dishes of the batch; {@code reservedQuantity}
 * is only ever changed through the atomic queries in {@link BatchRepository}.
 */
@Entity
@DynamicUpdate // never rewrite reserved_quantity from a stale copy; it is changed by atomic queries only
@Table(name = "batch")
@Getter
@Setter
@NoArgsConstructor
public class Batch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cook_id", nullable = false)
    private Cook cook;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    @Column(name = "order_cutoff", nullable = false)
    private Instant orderCutoff;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "pickup_start")
    private LocalTime pickupStart;

    @Column(name = "pickup_end")
    private LocalTime pickupEnd;

    @Column(name = "delivery_start")
    private LocalTime deliveryStart;

    @Column(name = "delivery_end")
    private LocalTime deliveryEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.OPEN;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "batch_dish", joinColumns = @JoinColumn(name = "batch_id"))
    @BatchSize(size = 50)
    private List<BatchDishItem> dishes = new ArrayList<>();

    public int remaining() {
        return Math.max(0, capacity - reservedQuantity);
    }

    public boolean isSoldOut() {
        return remaining() == 0;
    }

    public boolean supportsPickup() {
        return pickupStart != null && pickupEnd != null;
    }

    public boolean supportsDelivery() {
        return deliveryStart != null && deliveryEnd != null;
    }

    public boolean isOrderingOpen(Instant now) {
        return status == BatchStatus.OPEN && now.isBefore(orderCutoff) && !isSoldOut();
    }

    public BatchDishItem itemFor(java.util.UUID dishId) {
        return dishes.stream().filter(i -> i.getDish().getId().equals(dishId)).findFirst().orElse(null);
    }
}
