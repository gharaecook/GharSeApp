package com.gharse.delivery;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, UUID> {

    Optional<DeliveryOrder> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
