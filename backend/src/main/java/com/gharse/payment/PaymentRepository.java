package com.gharse.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
