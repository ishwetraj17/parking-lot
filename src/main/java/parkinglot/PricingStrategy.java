package parkinglot;

public interface PricingStrategy {
    long feeCents(VehicleType type, long entryTsMs, long exitTsMs);
}
