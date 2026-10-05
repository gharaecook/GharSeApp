package com.gharse.review;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByOrderId(UUID orderId);

    Page<Review> findByCookId(UUID cookId, Pageable pageable);

    @Query("select avg(r.rating) from Review r where r.cook.id = ?1")
    Double averageRating(UUID cookId);

    long countByCookId(UUID cookId);
}
