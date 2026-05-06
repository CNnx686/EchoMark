package org.tongji.sse.domain.strategy;

import java.util.Map;

public abstract class AbstractWeightStrategy {

    protected void increaseAdaptive(Map<String, Double> map, String key, double base, double cap) {
        if (key == null) return;
        double current = map.getOrDefault(key, 0.0);
        double factor = 1.0 - current / cap;
        if (factor <= 0) return;

        double delta = base * factor;
        map.put(key, Math.min(cap, current + delta));
    }

    protected void increaseLongAdaptive(Map<Long, Double> map, Long key, double base, double cap) {
        if (key == null) return;
        double current = map.getOrDefault(key, 0.0);
        double factor = 1.0 - current / cap;
        if (factor <= 0) return;

        double delta = base * factor;
        map.put(key, Math.min(cap, current + delta));
    }
}
