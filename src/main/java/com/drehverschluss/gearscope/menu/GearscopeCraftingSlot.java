package com.drehverschluss.gearscope.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/** A 2x2 crafting grid slot under Gearscope's own class name (see {@link GearscopeResultSlot}). */
public class GearscopeCraftingSlot extends Slot {
    public GearscopeCraftingSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }
}
