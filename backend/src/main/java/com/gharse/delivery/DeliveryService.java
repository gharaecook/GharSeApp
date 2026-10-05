package com.gharse.delivery;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private final DeliveryProvider provider;
    private final DeliveryOrderRepository repo;

    public DeliveryService(DeliveryProvider provider, DeliveryOrderRepository repo) {
        this.provider = provider;
        this.repo = repo;
    }

    public String providerName() {
        return provider.name();
    }

    public BigDecimal quote(String orderNumber, String pickupAddress, String dropAddress) {
        return provider.quote(new DeliveryProvider.Request(orderNumber, pickupAddress, dropAddress));
    }

    /** Books a courier with the provider and stores the delivery record. */
    @Transactional
    public DeliveryOrder book(UUID orderId, String orderNumber, String pickupAddress, String dropAddress, BigDecimal quoted) {
        DeliveryProvider.Booking b = provider.book(new DeliveryProvider.Request(orderNumber, pickupAddress, dropAddress));
        DeliveryOrder d = new DeliveryOrder();
        d.setOrderId(orderId);
        d.setProvider(provider.name());
        d.setProviderOrderId(b.providerOrderId());
        d.setPickupAddress(pickupAddress);
        d.setDropAddress(dropAddress);
        d.setStatus(DeliveryStatus.ASSIGNED);
        d.setTrackingUrl(b.trackingUrl());
        d.setQuotedCost(quoted);
        d.setActualCost(b.cost());
        return repo.save(d);
    }

    @Transactional
    public void markStatus(UUID orderId, DeliveryStatus status) {
        repo.findFirstByOrderIdOrderByCreatedAtDesc(orderId).ifPresent(d -> d.setStatus(status));
    }

    @Transactional
    public void cancel(UUID orderId) {
        repo.findFirstByOrderIdOrderByCreatedAtDesc(orderId).ifPresent(d -> {
            provider.cancel(d.getProviderOrderId());
            d.setStatus(DeliveryStatus.CANCELLED);
        });
    }

    public Optional<DeliveryOrder> forOrder(UUID orderId) {
        return repo.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
    }
}
