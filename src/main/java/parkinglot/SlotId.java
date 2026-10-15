package parkinglot;

import java.util.Objects;

public final class SlotId {
    private final int floor;
    private final int slot;
    private final SlotSize size;

    public SlotId(int floor, int slot, SlotSize size) {
        this.floor = floor;
        this.slot = slot;
        this.size = Objects.requireNonNull(size, "size");
    }

    public int floor() { return floor; }
    public int slot() { return slot; }
    public SlotSize size() { return size; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlotId)) return false;
        SlotId that = (SlotId) o;
        return floor == that.floor && slot == that.slot && size == that.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floor, slot, size);
    }

    @Override
    public String toString() {
        return "SlotId{f=" + floor + ",s=" + slot + "," + size + '}';
    }
}
