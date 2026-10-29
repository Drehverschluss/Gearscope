package com.drehverschluss.gearscope.core;

/**
 * Holds the currently installed accessory slot source (set by the Accessories compat when present) and the
 * slot placer (set by the client at startup).
 */
public final class GearscopeServices {
    private static volatile AccessorySlotSource slotSource = AccessorySlotSource.EMPTY;
    private static volatile AccessorySlotPlacer slotPlacer = AccessorySlotPlacer.NONE;

    private GearscopeServices() {
    }

    public static AccessorySlotSource slotSource() {
        return slotSource;
    }

    public static void setSlotSource(AccessorySlotSource source) {
        slotSource = source;
    }

    public static AccessorySlotPlacer slotPlacer() {
        return slotPlacer;
    }

    public static void setSlotPlacer(AccessorySlotPlacer placer) {
        slotPlacer = placer;
    }
}
