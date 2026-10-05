package com.gharse.menu;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DishRepository extends JpaRepository<Dish, UUID> {

    List<Dish> findByCookIdOrderByNameAsc(UUID cookId);

    List<Dish> findByCookIdAndActiveTrueOrderByNameAsc(UUID cookId);

    List<Dish> findByCookIdInAndActiveTrue(Collection<UUID> cookIds);

    long countByCookId(UUID cookId);
}
