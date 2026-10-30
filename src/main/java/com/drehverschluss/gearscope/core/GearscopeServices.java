package com.drehverschluss.gearscope.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntSupplier;

/**
 * Holds the currently installed accessory slot source (set by the Accessories compat when present), the slot placer
 * (set by the client at startup) and hooks other compats register.
 */
public final class GearscopeServices {
    private static final List<SlotClickHandler> SLOT_CLICK_HANDLERS = new CopyOnWriteArrayList<>();
    private static volatile AccessorySlotSource slotSource = AccessorySlotSource.EMPTY;
    private static volatile AccessorySlotPlacer slotPlacer = AccessorySlotPlacer.NONE;
    private static volatile IntSupplier openItemButton = () -> 1;

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

    /** Registered by compats on both sides; tried in order for plain clicks on Gearscope slots. */
    public static void addSlotClickHandler(SlotClickHandler handler) {
        SLOT_CLICK_HANDLERS.add(handler);
    }

    public static List<SlotClickHandler> slotClickHandlers() {
        return SLOT_CLICK_HANDLERS;
    }

    /**
     * The mouse button that opens a container item in place (0 = left, 1 = right). Right click like Quark's Expanded
     * Item Interactions; the Quark compat replaces it so Quark's "invert clicks" option applies everywhere.
     */
    public static IntSupplier openItemButton() {
        return openItemButton;
    }

    public static void setOpenItemButton(IntSupplier button) {
        openItemButton = button;
    }
}
