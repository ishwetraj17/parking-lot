package parkinglot;

import java.util.Objects;

public final class Ticket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final SlotId slot;
    private final long entryTsMs;

    public Ticket(String ticketId, Vehicle vehicle, SlotId slot, long entryTsMs) {
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.slot = Objects.requireNonNull(slot, "slot");
        this.entryTsMs = entryTsMs;
    }

    public String ticketId() { return ticketId; }
    public Vehicle vehicle() { return vehicle; }
    public SlotId slot() { return slot; }
    public long entryTsMs() { return entryTsMs; }

    @Override
    public String toString() {
        return "Ticket{" + ticketId + ", " + vehicle + ", " + slot + ", entry=" + entryTsMs + '}';
    }
}
