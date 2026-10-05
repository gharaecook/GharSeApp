package com.gharse.cook;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FavoriteRepository extends JpaRepository<FavoriteCook, FavoriteCook.Key> {

    @Query("select f.id.cookId from FavoriteCook f where f.id.customerId = ?1 order by f.createdAt desc")
    List<UUID> findCookIdsByCustomer(UUID customerId);
}
