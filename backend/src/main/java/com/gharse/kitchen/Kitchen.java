package com.gharse.kitchen;

import com.gharse.common.BaseEntity;
import com.gharse.cook.ApprovalStatus;
import com.gharse.cook.Cook;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "kitchen")
@Getter
@Setter
@NoArgsConstructor
public class Kitchen extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cook_id", nullable = false, unique = true)
    private Cook cook;

    private String address;

    private String locality;

    private Double latitude;

    private Double longitude;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private ApprovalStatus verificationStatus = ApprovalStatus.PENDING;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "kitchen_photo", joinColumns = @JoinColumn(name = "kitchen_id"))
    @Column(name = "url", nullable = false)
    @BatchSize(size = 50)
    private List<String> photos = new ArrayList<>();
}
