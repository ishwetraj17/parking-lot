package parkinglot;

import java.util.Objects;

public final class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = Objects.requireNonNull(licensePlate, "licensePlate");
        this.type = Objects.requireNonNull(type, "type");
        if (licensePlate.isEmpty()) {
            throw new IllegalArgumentException("licensePlate must be non-empty");
        }
    }

    public String licensePlate() { return licensePlate; }

    public VehicleType type() { return type; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle)) return false;
        Vehicle that = (Vehicle) o;
        return licensePlate.equals(that.licensePlate) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(licensePlate, type);
    }

    @Override
    public String toString() {
        return "Vehicle{" + licensePlate + "," + type + '}';
    }
}
