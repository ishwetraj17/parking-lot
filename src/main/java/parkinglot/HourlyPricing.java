package parkinglot;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Example pricing strategy: a flat hourly rate per VehicleType.
 * Partial hours round UP. exit &lt;= entry counts as zero hours (free).
 *
 * Candidates may extend this or replace it entirely in Phase 2.
 */
public final class HourlyPricing implements PricingStrategy {

    private final Map<VehicleType, Long> ratesPerHourCents;

    public HourlyPricing(Map<VehicleType, Long> ratesPerHourCents) {
        Objects.requireNonNull(ratesPerHourCents);
        EnumMap<VehicleType, Long> copy = new EnumMap<>(VehicleType.class);
        copy.putAll(ratesPerHourCents);
        for (VehicleType t : VehicleType.values()) {
            if (!copy.containsKey(t)) {
                throw new IllegalArgumentException("missing rate for " + t);
            }
            if (copy.get(t) < 0) {
                throw new IllegalArgumentException("negative rate for " + t);
            }
        }
        this.ratesPerHourCents = copy;
    }

    @Override
    public long feeCents(VehicleType type, long entryTsMs, long exitTsMs) {
        if (exitTsMs <= entryTsMs) return 0L;
        long durationMs = exitTsMs - entryTsMs;
        long hours = (durationMs + 3_600_000L - 1L) / 3_600_000L; // round up
        return hours * ratesPerHourCents.get(type);
    }
}
