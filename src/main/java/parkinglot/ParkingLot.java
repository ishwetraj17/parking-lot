package parkinglot;

import java.util.*;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;

public final class ParkingLot {

    private final Map<SlotSize, TreeSet<SlotId>> freeSlots = new EnumMap<>(SlotSize.class);
    private final Map<SlotSize, Integer> capacities = new EnumMap<>(SlotSize.class);
    private final Map<String, Ticket> activeTickets = new HashMap<>(), inactiveTickets = new HashMap<>();
    private final Map<String, String> parkedLicensePlates = new HashMap<>();

    private PricingStrategy pricing = (type, entryTsMs, exitTsMs) -> 0L;

    /**
     * Phase 1 / Phase 2: single-floor lot.
     */
    public ParkingLot(int smallCount, int mediumCount, int largeCount) {
        this(new int[][]{{smallCount, mediumCount, largeCount}});
        // TODO:
        //  - validate counts >= 0
        //  - initialize free-slot pools per SlotSize on floor 0
        //  - default to a "no pricing" strategy (fee = 0) until withPricing is called
//        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Phase 3: multi-floor lot. floorCounts[f] = {small, medium, large} counts for floor f.
     */
    public ParkingLot(int[][] floorCounts) {
        Objects.requireNonNull(floorCounts, "floorCounts");

        Comparator<SlotId> slotOrder = Comparator.comparingInt(SlotId::floor).thenComparingInt(SlotId::slot);
        for (SlotSize size : SlotSize.values()) {
            freeSlots.put(size, new TreeSet<>(slotOrder));
            capacities.put(size, 0);
        }
        for (int floor = 0; floor < floorCounts.length; floor++) {
            int[] counts = Objects.requireNonNull(floorCounts[floor], "floorCounts[" + floor + "]");

            if (counts.length != 3) {
                throw new IllegalArgumentException("floorCounts[" + floor + "] must have 3 elements");
            }
            for (int count : counts) {
                if (count < 0) {
                    throw new IllegalArgumentException("count must be >= 0");
                }
            }

            addFloorSlots(floor, SlotSize.SMALL, counts[0]);
            addFloorSlots(floor, SlotSize.MEDIUM, counts[1]);
            addFloorSlots(floor, SlotSize.LARGE, counts[2]);
        }

        // TODO (Phase 3)
//        throw new UnsupportedOperationException("TODO");
    }

    private void addFloorSlots(int floor, SlotSize size, int count) {
        TreeSet<SlotId> slots = freeSlots.get(size);

        for (int slot = 0; slot < count; slot++) {
            slots.add(new SlotId(floor, slot, size));
        }
        capacities.put(size, Math.addExact(capacities.get(size), count));
    }

    public synchronized ParkingLot withPricing(PricingStrategy pricing) {
        this.pricing = Objects.requireNonNull(pricing, "pricing");
        return this;
    }

    public synchronized Ticket park(Vehicle vehicle, long nowMs) {
        Objects.requireNonNull(vehicle, "vehicle");
        String licensePlate = vehicle.licensePlate();
        if (parkedLicensePlates.containsKey(licensePlate)) {
            throw new IllegalStateException("Vehicle is already parked :" + licensePlate);
        }
        SlotSize[] prefrences = switch (vehicle.type()) {
            case BIKE -> new SlotSize[]{SlotSize.SMALL, SlotSize.MEDIUM, SlotSize.LARGE};
            case CAR -> new SlotSize[]{SlotSize.MEDIUM, SlotSize.LARGE};
            case TRUCK -> new SlotSize[]{SlotSize.LARGE};
        };
        SlotId allocatedSlot = null;
        for (SlotSize size : prefrences) {
            TreeSet<SlotId> slots = freeSlots.get(size);
            if (!slots.isEmpty()) {
                allocatedSlot = slots.pollFirst();
                break;
            }
        }
        if (allocatedSlot == null) {
            throw new ParkingLotFullException("No suitable slot available for " + vehicle.type());
        }

        Ticket ticket = new Ticket(
                UUID.randomUUID().toString(), vehicle, allocatedSlot, nowMs
        );

        activeTickets.put(ticket.ticketId(), ticket);
        parkedLicensePlates.put(licensePlate, ticket.ticketId());

        return ticket;
    }

    public synchronized Receipt unpark(String ticketId, long nowMs) {
        if (ticketId == null) {
            throw new IllegalArgumentException("ticketId is null");
        }
        ;

        Ticket ticket = activeTickets.get(ticketId);

        if (ticket == null) {
            throw new IllegalArgumentException("Unknown or already used ticket: " + ticketId);
        }

        long feeCents = pricing.feeCents(ticket.vehicle().type(), ticket.entryTsMs(), nowMs);
        activeTickets.remove(ticketId);
        parkedLicensePlates.remove(ticket.vehicle().licensePlate());

        freeSlots.get(ticket.slot().size()).add(ticket.slot());

        return new Receipt(ticket.ticketId(), ticket.entryTsMs(), nowMs, feeCents);
    }

    public synchronized int availableSlots(SlotSize size) {
        Objects.requireNonNull(size, "size");
        return freeSlots.get(size).size();
    }

    public synchronized int capacity(SlotSize size) {
        Objects.requireNonNull(size, "size");
        return capacities.get(size);
    }
}
//    /**
//     * Phase 2: swap in a pricing strategy. Returns {@code this} for chaining.
//     */
//    public ParkingLot withPricing(PricingStrategy pricing) {
//        // TODO
//        throw new UnsupportedOperationException("TODO");
//    }
//
//    // ===== Phase 1 =====
//
//    /**
//     * Park a vehicle. Returns a ticket with the allocated slot.
//     *
//     * Allocation rules:
//     *  - BIKE  prefers SMALL, then MEDIUM, then LARGE
//     *  - CAR   prefers MEDIUM, then LARGE
//     *  - TRUCK requires LARGE
//     *
//     * When multiple slots of the chosen size are free, prefer the lowest floor,
//     * then the lowest slot index within that floor.
//     *
//     * @throws ParkingLotFullException if no suitable slot is available
//     * @throws IllegalStateException   if the vehicle (by license plate) is already parked
//     */
//    public Ticket park(Vehicle vehicle, long nowMs) {
//        // TODO
//        throw new UnsupportedOperationException("TODO");
//    }
//
//    /**
//     * Unpark a vehicle by its ticket. Frees the slot and returns a Receipt with the fee.
//     *
//     * @throws IllegalArgumentException if ticketId is unknown or already used
//     */
//    public Receipt unpark(String ticketId, long nowMs) {
//        // TODO
//        throw new UnsupportedOperationException("TODO");
//    }
//
//    /** Number of currently free slots of the given size, across all floors. */
//    public int availableSlots(SlotSize size) {
//        // TODO
//        throw new UnsupportedOperationException("TODO");
//    }
//
//    /** Total capacity for a slot size, across all floors. */
//    public int capacity(SlotSize size) {
//        // TODO
//        throw new UnsupportedOperationException("TODO");
//    }
//}
