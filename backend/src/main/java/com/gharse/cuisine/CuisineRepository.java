package com.gharse.cuisine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuisineRepository extends JpaRepository<Cuisine, UUID> {

    List<Cuisine> findAllByOrderByNameAsc();

    Optional<Cuisine> findByNameIgnoreCase(String name);
}
