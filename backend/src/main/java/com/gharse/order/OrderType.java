package com.gharse.order;

public enum OrderType {
    PICKUP, COOK_DELIVERY, THIRD_PARTY_DELIVERY;

    public boolean isDelivery() {
        return this != PICKUP;
    }
}
