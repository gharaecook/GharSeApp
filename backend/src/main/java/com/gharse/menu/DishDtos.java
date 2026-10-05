package com.gharse.menu;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class DishDtos {

    private DishDtos() {}

    public record DishRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 2000) String description,
            @NotNull DishCategory category,
            UUID cuisineId,
            @Size(max = 500) String imageUrl,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Size(max = 120) String portionDescription,
            @Min(0) Integer preparationTimeMinutes,
            @Size(max = 2000) String ingredients,
            @Size(max = 300) String allergens,
            Boolean vegetarian,
            Boolean active) {}

    public record DishResponse(UUID id, UUID cookId, String cookName, String name, String description,
                               DishCategory category, UUID cuisineId, String cuisine, String imageUrl, BigDecimal price,
                               String portionDescription, Integer preparationTimeMinutes, String ingredients,
                               String allergens, boolean vegetarian, boolean active) {

        public static DishResponse of(Dish d) {
            return new DishResponse(d.getId(), d.getCook().getId(), d.getCook().getUser().getName(), d.getName(),
                    d.getDescription(), d.getCategory(), d.getCuisine() == null ? null : d.getCuisine().getId(),
                    d.getCuisine() == null ? null : d.getCuisine().getName(), d.getImageUrl(), d.getPrice(),
                    d.getPortionDescription(), d.getPreparationTimeMinutes(), d.getIngredients(), d.getAllergens(),
                    d.isVegetarian(), d.isActive());
        }
    }
}
