package com.drehverschluss.gearscope.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * A plain (PICKUP) click on a Gearscope slot that another mod's feature takes over, e.g. opening a shulker box in
 * place. Runs on both sides with the same arguments, so client and server stay in sync; when it returns true the
 * normal slot click is skipped.
 */
@FunctionalInterface
public interface SlotClickHandler {
    boolean handleClick(AbstractContainerMenu menu, Slot slot, int button, Player player);
}
