package com.drehverschluss.gearscope.menu;

import com.drehverschluss.gearscope.core.GearscopeLayout;
import com.mojang.datafixers.util.Pair;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * The Gearscope character screen menu. Slot order (indices documented below) is fixed; later phases only
 * ever append new slots (Accessories) after {@link #HOTBAR_SLOT_END}, never reorder existing ones.
 */
public class GearscopeMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    public static final int CRAFT_SLOT_START = 1;
    public static final int CRAFT_SLOT_COUNT = 4;
    public static final int CRAFT_SLOT_END = CRAFT_SLOT_START + CRAFT_SLOT_COUNT;
    public static final int ARMOR_SLOT_START = CRAFT_SLOT_END;
    public static final int ARMOR_SLOT_COUNT = 4;
    public static final int ARMOR_SLOT_END = ARMOR_SLOT_START + ARMOR_SLOT_COUNT;
    public static final int OFFHAND_SLOT = ARMOR_SLOT_END;
    public static final int INV_SLOT_START = OFFHAND_SLOT + 1;
    public static final int INV_SLOT_COUNT = 27;
    public static final int INV_SLOT_END = INV_SLOT_START + INV_SLOT_COUNT;
    public static final int HOTBAR_SLOT_START = INV_SLOT_END;
    public static final int HOTBAR_SLOT_COUNT = 9;
    public static final int HOTBAR_SLOT_END = HOTBAR_SLOT_START + HOTBAR_SLOT_COUNT;

    // Armor order top to bottom in the left sidebar: head, chest, legs, feet.
    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final ResourceLocation[] ARMOR_EMPTY_ICONS = {
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
    };

    private final Player player;
    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots;

    public GearscopeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public GearscopeMenu(int containerId, Inventory playerInventory, @Nullable RegistryFriendlyByteBuf extraData) {
        super(GearscopeMenuTypes.CHARACTER.get(), containerId);
        this.player = playerInventory.player;
        this.craftSlots = new TransientCraftingContainer(this, 2, 2);
        this.resultSlots = new ResultContainer();

        // a) Crafting result + 2x2 grid.
        this.addSlot(new ResultSlot(this.player, this.craftSlots, this.resultSlots, 0,
                GearscopeLayout.RESULT_X, GearscopeLayout.RESULT_Y));
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                this.addSlot(new Slot(this.craftSlots, col + row * 2,
                        GearscopeLayout.CRAFT_X[col], GearscopeLayout.CRAFT_Y[row]));
            }
        }

        // b) Armor: head, chest, legs, feet. Reuses vanilla ArmorSlot (made public via access transformer)
        // so placement rules and curse-of-binding handling stay identical to the player inventory.
        for (int i = 0; i < ARMOR_ORDER.length; i++) {
            EquipmentSlot equipmentSlot = ARMOR_ORDER[i];
            int containerSlot = Inventory.INVENTORY_SIZE + equipmentSlot.getIndex();
            this.addSlot(new ArmorSlot(playerInventory, this.player, equipmentSlot, containerSlot,
                    GearscopeLayout.ARMOR_X, GearscopeLayout.ARMOR_Y[i], ARMOR_EMPTY_ICONS[i]));
        }

        // c) Offhand, mirroring the anonymous slot InventoryMenu uses for the shield slot.
        this.addSlot(new Slot(playerInventory, Inventory.SLOT_OFFHAND, GearscopeLayout.OFFHAND_X, GearscopeLayout.OFFHAND_Y) {
            @Override
            public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
                player.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
                super.setByPlayer(newStack, oldStack);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        // d) Inventory (3x9).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        GearscopeLayout.INV_X + col * GearscopeLayout.INV_SLOT_SIZE, GearscopeLayout.INV_ROW_Y[row]));
            }
        }

        // e) Hotbar.
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    GearscopeLayout.INV_X + col * GearscopeLayout.INV_SLOT_SIZE, GearscopeLayout.HOTBAR_Y));
        }
    }

    /**
     * Callback for when the crafting matrix is changed. Delegates to the vanilla crafting result logic
     * (access-transformed to public) instead of reimplementing it, so Polymorph's mixin on that exact
     * method keeps working automatically.
     */
    @Override
    public void slotsChanged(Container container) {
        CraftingMenu.slotChangedCraftingGrid(this, this.player.level(), this.player, this.craftSlots, this.resultSlots, null);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultSlots.clearContent();
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.craftSlots);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(stackInSlot);

            if (index == RESULT_SLOT) {
                if (!this.moveItemStackTo(stackInSlot, INV_SLOT_START, HOTBAR_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, result);
            } else if (index >= CRAFT_SLOT_START && index < CRAFT_SLOT_END) {
                if (!this.moveItemStackTo(stackInSlot, INV_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= ARMOR_SLOT_START && index < ARMOR_SLOT_END) {
                if (!this.moveItemStackTo(stackInSlot, INV_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == OFFHAND_SLOT) {
                if (!this.moveItemStackTo(stackInSlot, INV_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                    && !this.slots.get(ARMOR_SLOT_END - 1 - equipmentSlot.getIndex()).hasItem()) {
                int armorIndex = ARMOR_SLOT_END - 1 - equipmentSlot.getIndex();
                if (!this.moveItemStackTo(stackInSlot, armorIndex, armorIndex + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.slots.get(OFFHAND_SLOT).hasItem()) {
                if (!this.moveItemStackTo(stackInSlot, OFFHAND_SLOT, OFFHAND_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= INV_SLOT_START && index < INV_SLOT_END) {
                if (!this.moveItemStackTo(stackInSlot, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= HOTBAR_SLOT_START && index < HOTBAR_SLOT_END) {
                if (!this.moveItemStackTo(stackInSlot, INV_SLOT_START, INV_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, INV_SLOT_START, HOTBAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY, result);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
            if (index == RESULT_SLOT) {
                player.drop(stackInSlot, false);
            }
        }

        return result;
    }
}
