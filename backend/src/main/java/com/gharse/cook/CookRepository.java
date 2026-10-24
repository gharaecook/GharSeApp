package com.gharse.cook;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CookRepository extends JpaRepository<Cook, UUID>, JpaSpecificationExecutor<Cook> {

    Optional<Cook> findByUserId(UUID userId);

    List<Cook> findByApprovalStatus(ApprovalStatus status);

    Page<Cook> findByApprovalStatus(ApprovalStatus status, Pageable pageable);

    long countByApprovalStatus(ApprovalStatus status);
}
