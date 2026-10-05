package com.gharse.menu;

import com.gharse.common.BaseEntity;
import com.gharse.cook.Cook;
import com.gharse.cuisine.Cuisine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dish")
@Getter
@Setter
@NoArgsConstructor
public class Dish extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cook_id", nullable = false)
    private Cook cook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuisine_id")
    private Cuisine cuisine;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DishCategory category = DishCategory.MEAL;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "portion_description")
    private String portionDescription;

    @Column(name = "preparation_time_minutes")
    private Integer preparationTimeMinutes;

    @Column(columnDefinition = "text")
    private String ingredients;

    private String allergens;

    @Column(name = "image_url")
    private String imageUrl;

    private boolean vegetarian = true;

    private boolean active = true;
}
