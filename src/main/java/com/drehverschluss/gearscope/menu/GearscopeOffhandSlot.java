package com.drehverschluss.gearscope.menu;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The offhand slot, mirroring the anonymous slot {@link InventoryMenu} uses for the shield slot, under
 * Gearscope's own class name (see {@link GearscopeResultSlot}).
 */
public class GearscopeOffhandSlot extends Slot {
    private final Player player;

    public GearscopeOffhandSlot(Inventory playerInventory, int x, int y) {
        super(playerInventory, Inventory.SLOT_OFFHAND, x, y);
        this.player = playerInventory.player;
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        this.player.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
        super.setByPlayer(newStack, oldStack);
    }

    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
    }
}
