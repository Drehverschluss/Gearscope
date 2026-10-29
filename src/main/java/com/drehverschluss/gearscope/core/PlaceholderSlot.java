package com.drehverschluss.gearscope.core;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot that is never shown, never accepts items and holds nothing. Used to keep slot indices aligned
 * when a container cannot be resolved.
 */
public class PlaceholderSlot extends Slot {
    public PlaceholderSlot(int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
