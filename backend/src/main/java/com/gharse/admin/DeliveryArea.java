package com.gharse.admin;

import com.gharse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_area")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryArea extends BaseEntity {

    @Column(name = "place_id", nullable = false, unique = true, length = 200)
    private String placeId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "formatted_address", nullable = false, length = 500)
    private String formattedAddress;

    @Column(nullable = false, length = 120)
    private String locality;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryAreaStatus status = DeliveryAreaStatus.NOT_STARTED;
}
