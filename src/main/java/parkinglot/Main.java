package parkinglot;

import java.util.Map;

public class Main {

    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot(2, 2, 1)
                .withPricing(new HourlyPricing(Map.of(
                        VehicleType.BIKE,  20L,
                        VehicleType.CAR,   50L,
                        VehicleType.TRUCK, 100L)));

        long t0 = 0L;

        Ticket bike  = lot.park(new Vehicle("KA-01-B-1", VehicleType.BIKE),  t0);
        Ticket car1  = lot.park(new Vehicle("KA-01-C-1", VehicleType.CAR),   t0);
        Ticket car2  = lot.park(new Vehicle("KA-01-C-2", VehicleType.CAR),   t0);
        Ticket truck = lot.park(new Vehicle("KA-01-T-1", VehicleType.TRUCK), t0);

        System.out.println(bike);
        System.out.println(car1);
        System.out.println(car2);
        System.out.println(truck);

        // 2.5 hours later → 3 billed hours
        long t1 = 2L * 3_600_000L + 1_800_000L;
        System.out.println(lot.unpark(car1.ticketId(), t1));

        System.out.println("free SMALL=" + lot.availableSlots(SlotSize.SMALL)
                + " MEDIUM=" + lot.availableSlots(SlotSize.MEDIUM)
                + " LARGE="  + lot.availableSlots(SlotSize.LARGE));
    }
}
