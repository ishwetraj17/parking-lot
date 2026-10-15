import parkinglot.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ParkingLotTest {

    // ===== Phase 1 =====

    @Test
    void park_bike_prefersSmallSlot() {
        ParkingLot lot = new ParkingLot(2, 1, 1);
        Ticket t = lot.park(new Vehicle("B1", VehicleType.BIKE), 0L);
        assertEquals(SlotSize.SMALL, t.slot().size());
        assertEquals(1, lot.availableSlots(SlotSize.SMALL));
    }

    @Test
    void park_car_takesMediumWhenAvailable() {
        ParkingLot lot = new ParkingLot(1, 1, 1);
        Ticket t = lot.park(new Vehicle("C1", VehicleType.CAR), 0L);
        assertEquals(SlotSize.MEDIUM, t.slot().size());
    }

    @Test
    void park_bike_fallsBackToMediumWhenSmallFull() {
        ParkingLot lot = new ParkingLot(1, 1, 0);
        lot.park(new Vehicle("B1", VehicleType.BIKE), 0L); // takes SMALL
        Ticket t2 = lot.park(new Vehicle("B2", VehicleType.BIKE), 0L); // SMALL full → MEDIUM
        assertEquals(SlotSize.MEDIUM, t2.slot().size());
    }

    @Test
    void park_car_fallsBackToLargeWhenMediumFull() {
        ParkingLot lot = new ParkingLot(0, 1, 1);
        lot.park(new Vehicle("C1", VehicleType.CAR), 0L);   // MEDIUM
        Ticket t2 = lot.park(new Vehicle("C2", VehicleType.CAR), 0L);
        assertEquals(SlotSize.LARGE, t2.slot().size());
    }

    @Test
    void park_truck_requiresLarge_neverShrinks() {
        ParkingLot lot = new ParkingLot(5, 5, 0);
        assertThrows(ParkingLotFullException.class,
                () -> lot.park(new Vehicle("T1", VehicleType.TRUCK), 0L));
    }

    @Test
    void park_throwsWhenFull() {
        ParkingLot lot = new ParkingLot(0, 0, 1);
        lot.park(new Vehicle("T1", VehicleType.TRUCK), 0L);
        assertThrows(ParkingLotFullException.class,
                () -> lot.park(new Vehicle("T2", VehicleType.TRUCK), 0L));
    }

    @Test
    void park_rejectsDoubleParkingSameLicensePlate() {
        ParkingLot lot = new ParkingLot(2, 0, 0);
        lot.park(new Vehicle("B1", VehicleType.BIKE), 0L);
        assertThrows(IllegalStateException.class,
                () -> lot.park(new Vehicle("B1", VehicleType.BIKE), 0L));
    }

    @Test
    void unpark_freesTheSlot() {
        ParkingLot lot = new ParkingLot(1, 0, 0);
        Ticket t = lot.park(new Vehicle("B1", VehicleType.BIKE), 0L);
        assertEquals(0, lot.availableSlots(SlotSize.SMALL));

        Receipt r = lot.unpark(t.ticketId(), 1000L);
        assertEquals(t.ticketId(), r.ticketId());
        assertEquals(1, lot.availableSlots(SlotSize.SMALL));

        // re-parking the same vehicle now works
        assertDoesNotThrow(() -> lot.park(new Vehicle("B1", VehicleType.BIKE), 2000L));
    }

    @Test
    void unpark_unknownTicket_throws() {
        ParkingLot lot = new ParkingLot(1, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> lot.unpark("nope", 0L));
    }

    @Test
    void unpark_alreadyUsedTicket_throws() {
        ParkingLot lot = new ParkingLot(1, 0, 0);
        Ticket t = lot.park(new Vehicle("B1", VehicleType.BIKE), 0L);
        lot.unpark(t.ticketId(), 1000L);
        assertThrows(IllegalArgumentException.class, () -> lot.unpark(t.ticketId(), 2000L));
    }

    @Test
    void capacity_andAvailableSlots_areConsistent() {
        ParkingLot lot = new ParkingLot(3, 2, 1);
        assertEquals(3, lot.capacity(SlotSize.SMALL));
        assertEquals(2, lot.capacity(SlotSize.MEDIUM));
        assertEquals(1, lot.capacity(SlotSize.LARGE));

        assertEquals(3, lot.availableSlots(SlotSize.SMALL));
        lot.park(new Vehicle("B1", VehicleType.BIKE), 0L);
        assertEquals(2, lot.availableSlots(SlotSize.SMALL));
    }

    // ===== Phase 2 — Pricing =====

    @Test
    void pricing_chargesByVehicleType_partialHourRoundsUp() {
        ParkingLot lot = new ParkingLot(1, 1, 1)
                .withPricing(new HourlyPricing(Map.of(
                        VehicleType.BIKE,  20L,
                        VehicleType.CAR,   50L,
                        VehicleType.TRUCK, 100L)));

        Ticket bike = lot.park(new Vehicle("B1", VehicleType.BIKE), 0L);
        // 1 minute → rounds up to 1 hour
        assertEquals(20L, lot.unpark(bike.ticketId(), 60_000L).feeCents());

        Ticket car = lot.park(new Vehicle("C1", VehicleType.CAR), 0L);
        // 2h30m → 3 hours
        assertEquals(150L, lot.unpark(car.ticketId(),
                2L * 3_600_000L + 1_800_000L).feeCents());

        Ticket truck = lot.park(new Vehicle("T1", VehicleType.TRUCK), 0L);
        // exactly 1 hour → 1 hour
        assertEquals(100L, lot.unpark(truck.ticketId(), 3_600_000L).feeCents());
    }

    @Test
    void pricing_zeroDurationIsFree() {
        ParkingLot lot = new ParkingLot(1, 0, 0)
                .withPricing(new HourlyPricing(Map.of(
                        VehicleType.BIKE,  20L,
                        VehicleType.CAR,   50L,
                        VehicleType.TRUCK, 100L)));
        Ticket t = lot.park(new Vehicle("B1", VehicleType.BIKE), 1000L);
        assertEquals(0L, lot.unpark(t.ticketId(), 1000L).feeCents());
    }

    @Test
    void noPricingConfigured_feeIsZero() {
        ParkingLot lot = new ParkingLot(1, 0, 0);
        Ticket t = lot.park(new Vehicle("B1", VehicleType.BIKE), 0L);
        assertEquals(0L, lot.unpark(t.ticketId(), 10_000_000L).feeCents());
    }

    // ===== Phase 3 — Multi-floor =====

    @Test
    void multiFloor_prefersLowestFloorThenLowestSlot() {
        // Floor 0: 1 small. Floor 1: 1 small.
        ParkingLot lot = new ParkingLot(new int[][] {
                {1, 0, 0},
                {1, 0, 0},
        });

        Ticket t1 = lot.park(new Vehicle("B1", VehicleType.BIKE), 0L);
        Ticket t2 = lot.park(new Vehicle("B2", VehicleType.BIKE), 0L);

        assertEquals(0, t1.slot().floor(), "first bike → floor 0");
        assertEquals(1, t2.slot().floor(), "second bike → floor 1 (0 is full)");
    }

    @Test
    void multiFloor_freedLowFloorIsReusedBeforeHigh() {
        ParkingLot lot = new ParkingLot(new int[][] {
                {1, 0, 0},
                {1, 0, 0},
        });
        Ticket f0 = lot.park(new Vehicle("B1", VehicleType.BIKE), 0L); // floor 0
        Ticket f1 = lot.park(new Vehicle("B2", VehicleType.BIKE), 0L); // floor 1

        lot.unpark(f0.ticketId(), 1000L);

        Ticket again = lot.park(new Vehicle("B3", VehicleType.BIKE), 2000L);
        assertEquals(0, again.slot().floor(), "freed floor-0 slot must be preferred");
    }

    @Test
    void concurrentParks_doNotDoubleAllocateSameSlot() throws Exception {
        int totalSmall = 50;
        ParkingLot lot = new ParkingLot(totalSmall, 0, 0);

        int threads = 16;
        int attempts = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger parked = new AtomicInteger();
        AtomicInteger full = new AtomicInteger();
        ConcurrentHashMap<SlotId, String> seen = new ConcurrentHashMap<>();

        try {
            CountDownLatch start = new CountDownLatch(1);
            for (int i = 0; i < attempts; i++) {
                int id = i;
                pool.submit(() -> {
                    try {
                        start.await();
                        Ticket t = lot.park(
                                new Vehicle("B-" + id, VehicleType.BIKE), 0L);
                        if (seen.putIfAbsent(t.slot(), t.ticketId()) != null) {
                            fail("slot " + t.slot() + " allocated twice");
                        }
                        parked.incrementAndGet();
                    } catch (ParkingLotFullException ignored) {
                        full.incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        assertEquals(totalSmall, parked.get(), "exactly capacity slots parked");
        assertEquals(attempts - totalSmall, full.get(), "rest got ParkingLotFullException");
    }
}
