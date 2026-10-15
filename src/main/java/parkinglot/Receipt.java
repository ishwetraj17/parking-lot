package parkinglot;

import java.util.Objects;

public final class Receipt {
    private final String ticketId;
    private final long entryTsMs;
    private final long exitTsMs;
    private final long feeCents;

    public Receipt(String ticketId, long entryTsMs, long exitTsMs, long feeCents) {
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.entryTsMs = entryTsMs;
        this.exitTsMs = exitTsMs;
        this.feeCents = feeCents;
    }

    public String ticketId() { return ticketId; }
    public long entryTsMs() { return entryTsMs; }
    public long exitTsMs() { return exitTsMs; }
    public long feeCents() { return feeCents; }

    @Override
    public String toString() {
        return "Receipt{" + ticketId + ", entry=" + entryTsMs + ", exit=" + exitTsMs
                + ", fee=" + feeCents + "c}";
    }
}
