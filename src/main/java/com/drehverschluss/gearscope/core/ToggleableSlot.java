package com.drehverschluss.gearscope.core;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;

/**
 * Implemented by Gearscope's accessory slots. The screen decides on the client whether a slot is visible
 * (current tab, page, cosmetic mode); the server menu never sets this and keeps every slot active.
 */
public interface ToggleableSlot {
    void gearscope$setVisibility(BooleanSupplier visibility);

    boolean gearscope$isCosmetic();

    /**
     * Tooltip shown when hovering the empty slot (e.g. "Slot: Ring").
     */
    List<Component> gearscope$emptySlotTooltip();
}
