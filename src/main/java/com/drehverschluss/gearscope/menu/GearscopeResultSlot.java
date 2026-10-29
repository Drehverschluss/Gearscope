package com.drehverschluss.gearscope.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;

/**
 * Vanilla crafting result slot under Gearscope's own class name, so mods that match slots by exact class
 * (Inventory Sorter's blacklist) can exclude it without affecting other screens.
 */
public class GearscopeResultSlot extends ResultSlot {
    public GearscopeResultSlot(Player player, CraftingContainer craftSlots, Container container, int slot, int x, int y) {
        super(player, craftSlots, container, slot, x, y);
    }
}
