package com.gharse.common;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Simple fixed-window per-key limiter, adequate for a single-instance MVP. */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private record Window(long minute, int count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AppProperties props;
    private final Clock clock;

    public InMemoryRateLimiter(AppProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire(String key) {
        if (!props.rateLimit().enabled()) return true;
        long minute = clock.millis() / 60_000;
        int limit = props.rateLimit().authRequestsPerMinute();
        if (windows.size() > 10_000) windows.clear();
        Window w = windows.merge(key, new Window(minute, 1),
                (old, fresh) -> old.minute() == minute ? new Window(minute, old.count() + 1) : fresh);
        return w.count() <= limit;
    }
}
