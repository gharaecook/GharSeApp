package com.gharse.admin;

import com.gharse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "delivery_area")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryArea extends BaseEntity {

    @Column(name = "place_id", unique = true, length = 200)
    private String placeId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "formatted_address", length = 500)
    private String formattedAddress;

    @Column(length = 120)
    private String locality;

    @Column(length = 120)
    private String city;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_type", nullable = false, length = 16)
    private DeliveryAreaType areaType = DeliveryAreaType.PLACE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<DeliveryAreaPoint> boundary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryAreaStatus status = DeliveryAreaStatus.NOT_SUPPORTED;
}
