package com.gharse.availability;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchRepository extends JpaRepository<Batch, UUID> {

    List<Batch> findByCookIdOrderByAvailableDateDescCreatedAtDesc(UUID cookId);

    @Query("select b from Batch b where b.cook.id = :cookId and b.status = :status and b.availableDate >= :from "
            + "order by b.availableDate, b.orderCutoff")
    List<Batch> findUpcoming(@Param("cookId") UUID cookId, @Param("status") BatchStatus status, @Param("from") LocalDate from);

    @Query("select b from Batch b join fetch b.cook c join fetch c.user where b.status = :status "
            + "and b.availableDate >= :from and c.approvalStatus = com.gharse.cook.ApprovalStatus.APPROVED "
            + "and c.user.status = com.gharse.user.UserStatus.ACTIVE "
            + "and b.orderCutoff > :now order by b.availableDate, b.orderCutoff")
    List<Batch> findOpenForDiscovery(@Param("status") BatchStatus status, @Param("from") LocalDate from, @Param("now") Instant now);

    @Query("select distinct b.cook.id from Batch b where b.cook.id in :cookIds and b.availableDate = :today "
            + "and b.status = :status and b.reservedQuantity < b.capacity and b.orderCutoff > :now")
    Set<UUID> findCookIdsWithBatchToday(@Param("cookIds") Collection<UUID> cookIds, @Param("today") LocalDate today,
                                        @Param("status") BatchStatus status, @Param("now") Instant now);

    /**
     * Atomically reserves {@code qty} portions. Returns 1 when the batch is open, before cut-off and still has the
     * capacity; 0 otherwise. A single UPDATE keeps concurrent orders from overselling.
     */
    @Modifying
    @Query("update Batch b set b.reservedQuantity = b.reservedQuantity + :qty where b.id = :id "
            + "and b.status = com.gharse.availability.BatchStatus.OPEN and b.orderCutoff > :now "
            + "and b.reservedQuantity + :qty <= b.capacity")
    int reserve(@Param("id") UUID id, @Param("qty") int qty, @Param("now") Instant now);

    @Modifying
    @Query("update Batch b set b.reservedQuantity = b.reservedQuantity - :qty where b.id = :id and b.reservedQuantity >= :qty")
    int release(@Param("id") UUID id, @Param("qty") int qty);

    long countByCookIdAndStatusAndAvailableDateGreaterThanEqual(UUID cookId, BatchStatus status, LocalDate from);
}
