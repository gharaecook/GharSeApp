package com.gharse.common;

/** Extension point: swap the in-memory implementation for Redis/gateway based limiting later. */
public interface RateLimiter {

    /** @return true when the call identified by {@code key} is allowed. */
    boolean tryAcquire(String key);
}
