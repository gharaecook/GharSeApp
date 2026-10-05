package com.gharse.availability;

import com.gharse.menu.Dish;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A dish offered in a batch, with the batch-specific price (row of batch_dish). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchDishItem {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
