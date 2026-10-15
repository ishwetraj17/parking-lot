# Parking Lot – Machine Coding Exercise

## Overview
You are given a partially implemented **parking lot system**. Some methods are intentionally left incomplete and **unit tests are failing**.

Your task is to **complete the implementation so that all enabled tests pass**, phase by phase.

This exercise evaluates:
- Domain modeling (vehicles, slots, tickets, receipts)
- Allocation policy under fallback rules
- Pluggable strategy (pricing)
- Concurrency correctness for the multi-floor allocator (Phase 3)

You may use the internet for **Java syntax and documentation**, but **LLMs are not allowed**.

---

## Domain

| Vehicle | Preferred slot order |
|---|---|
| BIKE  | SMALL → MEDIUM → LARGE |
| CAR   | MEDIUM → LARGE |
| TRUCK | LARGE only |

A vehicle is identified by its license plate. A given license plate cannot be parked twice simultaneously.

When multiple slots of the chosen size are free, pick:
- the lowest floor first
- then the lowest slot index within that floor

---

## Phase 1 – Park, Unpark, Capacity

### Constructor
```java
new ParkingLot(int small, int medium, int large)
```
Single-floor lot, all slots on floor `0`.

### `Ticket park(Vehicle v, long nowMs)`
- Allocate per the table above
- Throw `ParkingLotFullException` if no suitable slot
- Throw `IllegalStateException` if the same license plate is currently parked
- Ticket carries a unique `ticketId`, the allocated `SlotId`, and `entryTsMs = nowMs`

### `Receipt unpark(String ticketId, long nowMs)`
- Free the slot
- Throw `IllegalArgumentException` if ticketId is unknown or already used
- Fee comes from the configured `PricingStrategy` (default: 0 if none configured)

### `int availableSlots(SlotSize)` and `int capacity(SlotSize)`
- Across all floors.

---

## Phase 2 – Pricing

```java
ParkingLot withPricing(PricingStrategy pricing)
```

`PricingStrategy.feeCents(VehicleType type, long entryMs, long exitMs)` returns the fee in cents.

A reference `HourlyPricing` is provided:
- per-VehicleType hourly rate
- partial hours round **up**
- exit ≤ entry → fee = 0

You do not need to write your own strategy unless the candidate wants to.

---

## Phase 3 – Multi-floor lot

### Constructor
```java
new ParkingLot(int[][] floorCounts)
// floorCounts[f] = {small, medium, large} for floor f
```

### Requirements
- All Phase 1 + 2 behavior still holds
- Allocation order: **lowest floor first, then lowest slot index** — same rule as before, now across floors
- A freed lower-floor slot must be preferred over a still-free higher-floor slot
- `park()` and `unpark()` must be **thread-safe**:
    - No two threads can be issued tickets pointing to the same `SlotId`
    - No lost frees, no `ConcurrentModificationException`
    - When the lot is full, concurrent `park()` calls cleanly throw `ParkingLotFullException` instead of blocking

The concurrent test in `ParkingLotTest` fires 200 parks at a 50-slot lot with 16 threads — exactly 50 succeed, 150 throw, and no two tickets share a slot.

## Constraints
- No external libraries beyond JUnit
- Java 17
- Correctness first

Run:
```bash
mvn test
```
