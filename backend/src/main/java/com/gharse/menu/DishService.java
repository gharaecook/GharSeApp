package com.gharse.menu;

import com.gharse.common.ApiException;
import com.gharse.cook.Cook;
import com.gharse.cook.CookService;
import com.gharse.cuisine.CuisineRepository;
import com.gharse.menu.DishDtos.DishRequest;
import com.gharse.menu.DishDtos.DishResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DishService {

    private final DishRepository dishes;
    private final CuisineRepository cuisines;
    private final CookService cookService;

    public DishService(DishRepository dishes, CuisineRepository cuisines, CookService cookService) {
        this.dishes = dishes;
        this.cuisines = cuisines;
        this.cookService = cookService;
    }

    // public
    public List<DishResponse> publicDishesOf(UUID cookId) {
        cookService.getApproved(cookId);
        return dishes.findByCookIdAndActiveTrueOrderByNameAsc(cookId).stream().map(DishResponse::of).toList();
    }

    public DishResponse publicDish(UUID id) {
        Dish d = dishes.findById(id).filter(Dish::isActive).orElseThrow(() -> ApiException.notFound("Dish"));
        cookService.getApproved(d.getCook().getId());
        return DishResponse.of(d);
    }

    // cook
    public List<DishResponse> myDishes(UUID userId) {
        Cook c = cookService.requireCook(userId);
        return dishes.findByCookIdOrderByNameAsc(c.getId()).stream().map(DishResponse::of).toList();
    }

    @Transactional
    public DishResponse create(UUID userId, DishRequest r) {
        Cook c = cookService.requireApprovedCook(userId);
        Dish d = new Dish();
        d.setCook(c);
        apply(d, r);
        return DishResponse.of(dishes.save(d));
    }

    @Transactional
    public DishResponse update(UUID userId, UUID dishId, DishRequest r) {
        Dish d = owned(userId, dishId);
        apply(d, r);
        return DishResponse.of(dishes.save(d));
    }

    /** Deactivates (soft delete) so historical orders and batches stay intact. */
    @Transactional
    public void deactivate(UUID userId, UUID dishId) {
        Dish d = owned(userId, dishId);
        d.setActive(false);
    }

    private Dish owned(UUID userId, UUID dishId) {
        Cook c = cookService.requireCook(userId);
        Dish d = dishes.findById(dishId).orElseThrow(() -> ApiException.notFound("Dish"));
        if (!d.getCook().getId().equals(c.getId())) throw ApiException.forbidden("You can only modify your own dishes");
        return d;
    }

    private void apply(Dish d, DishRequest r) {
        d.setName(r.name().trim());
        d.setDescription(r.description());
        d.setCategory(r.category());
        d.setCuisine(r.cuisineId() == null ? null : cuisines.findById(r.cuisineId())
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_CUISINE", "Cuisine does not exist")));
        d.setImageUrl(r.imageUrl());
        d.setPrice(r.price());
        d.setPortionDescription(r.portionDescription());
        d.setPreparationTimeMinutes(r.preparationTimeMinutes());
        d.setIngredients(r.ingredients());
        d.setAllergens(r.allergens());
        if (r.vegetarian() != null) d.setVegetarian(r.vegetarian());
        if (r.active() != null) d.setActive(r.active());
    }
}
