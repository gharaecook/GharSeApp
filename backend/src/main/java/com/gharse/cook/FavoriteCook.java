package com.gharse.cook;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "favorite_cook")
@Getter
@Setter
@NoArgsConstructor
public class FavoriteCook {

    @Embeddable
    public record Key(@Column(name = "customer_id") UUID customerId, @Column(name = "cook_id") UUID cookId)
            implements Serializable {}

    @EmbeddedId
    private Key id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public FavoriteCook(UUID customerId, UUID cookId, Instant createdAt) {
        this.id = new Key(customerId, cookId);
        this.createdAt = createdAt;
    }
}
