package com.gharse.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAreaRepository extends JpaRepository<DeliveryArea, UUID> {
    List<DeliveryArea> findAllByOrderByCityAscLocalityAsc();
    Optional<DeliveryArea> findByPlaceId(String placeId);
}
