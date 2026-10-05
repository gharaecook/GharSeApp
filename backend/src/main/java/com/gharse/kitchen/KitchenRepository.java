package com.gharse.kitchen;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenRepository extends JpaRepository<Kitchen, UUID> {

    Optional<Kitchen> findByCookId(UUID cookId);

    List<Kitchen> findByCookIdIn(Collection<UUID> cookIds);
}
