package com.gharse.cook;

import com.gharse.common.BaseEntity;
import com.gharse.cuisine.Cuisine;
import com.gharse.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "cook")
@Getter
@Setter
@NoArgsConstructor
public class Cook extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Short speciality line, e.g. "Authentic Kolhapuri Food". */
    private String headline;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "years_of_cooking")
    private Integer yearsOfCooking;

    private String locality;

    private String city = "Pune";

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    /** Comma separated signature dishes, e.g. "Puran Poli, Misal, Thali". */
    @Column(name = "signature_dishes")
    private String signatureDishes;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cook_cuisine", joinColumns = @JoinColumn(name = "cook_id"),
            inverseJoinColumns = @JoinColumn(name = "cuisine_id"))
    @BatchSize(size = 50)
    private Set<Cuisine> cuisines = new HashSet<>();

    public boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }
}
