package com.drehverschluss.gearscope.menu;

import com.drehverschluss.gearscope.core.AccessoryPlacement;
import com.drehverschluss.gearscope.core.AccessorySlotMeta;
import com.drehverschluss.gearscope.core.AccessorySlotPlacer;
import com.drehverschluss.gearscope.core.AccessorySlotSource;
import com.drehverschluss.gearscope.core.AccessorySlotSpec;
import com.drehverschluss.gearscope.core.GearscopeLayout;
import com.drehverschluss.gearscope.core.GearscopeServices;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * The Gearscope character screen menu. Slot order (indices documented below) is fixed; new slots are only
 * ever appended, never reordered:
 * <ul>
 * <li>0-45: vanilla slots (Phase 1), see the constants below.</li>
 * <li>46+: Accessories slots (Phase 2), one functional and one cosmetic slot per slot index, in the order
 * of {@link #accessorySpecs()} which the server sends to the client.</li>
 * <li>Then, if the server enabled it, 4 cosmetic armor slots (head, chest, legs, feet) at the positions of
 * the vanilla armor slots; the screen shows either those or the vanilla ones.</li>
 * </ul>
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
    public static final int ACCESSORY_SLOT_START = HOTBAR_SLOT_END;

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

    /** An accessory slot of this menu together with what it is and where the client draws it. */
    public record AccessoryEntry(Slot slot, AccessorySlotMeta meta, AccessoryPlacement placement) {
    }

    private final Player player;
    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private final List<AccessorySlotSpec> accessorySpecs;
    private final List<AccessoryEntry> accessoryEntries = new ArrayList<>();
    private final List<Slot> armorSlots = new ArrayList<>();
    private final List<Slot> cosmeticArmorSlots = new ArrayList<>();
    // End of all appended Accessories slots (accessory slots and cosmetic armor).
    private final int accessorySlotEnd;
    // Functional accessory slot indices in shift-click order; the client may send a better order.
    private int[] accessoryQuickMoveOrder;

    /** Client side: built from the slot list the server wrote when opening the menu. */
    public static GearscopeMenu fromNetwork(int containerId, Inventory playerInventory, @Nullable RegistryFriendlyByteBuf extraData) {
        List<AccessorySlotSpec> specs = extraData != null ? AccessorySlotSpec.readList(extraData) : List.of();
        boolean cosmeticArmor = extraData != null && extraData.readBoolean();
        return new GearscopeMenu(containerId, playerInventory, specs, cosmeticArmor);
    }

    public GearscopeMenu(int containerId, Inventory playerInventory, List<AccessorySlotSpec> accessorySpecs, boolean cosmeticArmor) {
        super(GearscopeMenuTypes.CHARACTER.get(), containerId);
        this.player = playerInventory.player;
        this.craftSlots = new TransientCraftingContainer(this, 2, 2);
        this.resultSlots = new ResultContainer();
        this.accessorySpecs = List.copyOf(accessorySpecs);
        // Slot positions only matter on the client (layout.json there); the server uses the defaults.
        GearscopeLayout layout = GearscopeLayout.current();

        // a) Crafting result + 2x2 grid.
        this.addSlot(new ResultSlot(this.player, this.craftSlots, this.resultSlots, 0,
                layout.resultX, layout.resultY));
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                this.addSlot(new Slot(this.craftSlots, col + row * 2,
                        layout.craftX[col], layout.craftY[row]));
            }
        }

        // b) Armor: head, chest, legs, feet. Reuses vanilla ArmorSlot (made public via access transformer)
        // so placement rules and curse-of-binding handling stay identical to the player inventory. The
        // screen hides them while cosmetic armor is shown in their place.
        for (int i = 0; i < ARMOR_ORDER.length; i++) {
            EquipmentSlot equipmentSlot = ARMOR_ORDER[i];
            int containerSlot = Inventory.INVENTORY_SIZE + equipmentSlot.getIndex();
            this.armorSlots.add(this.addSlot(new ToggleableArmorSlot(playerInventory, this.player, equipmentSlot, containerSlot,
                    layout.armorX, layout.armorY[i], ARMOR_EMPTY_ICONS[i])));
        }

        // c) Offhand, mirroring the anonymous slot InventoryMenu uses for the shield slot.
        this.addSlot(new Slot(playerInventory, Inventory.SLOT_OFFHAND, layout.offhandX, layout.offhandY) {
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
                        layout.inventoryX + col * layout.slotStep, layout.inventoryRowY[row]));
            }
        }

        // e) Hotbar.
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    layout.inventoryX + col * layout.slotStep, layout.hotbarY));
        }

        // f) Accessories: functional + cosmetic slot per slot index.
        this.addAccessorySlots();
        // g) Cosmetic armor at the vanilla armor positions (Accessories' hidden armor slot types).
        if (cosmeticArmor) {
            AccessorySlotSource source = GearscopeServices.slotSource();
            for (int i = 0; i < ARMOR_ORDER.length; i++) {
                this.cosmeticArmorSlots.add(this.addSlot(source.createCosmeticArmorSlot(
                        this.player, ARMOR_ORDER[i], layout.armorX, layout.armorY[i])));
            }
        }
        this.accessorySlotEnd = this.slots.size();
        this.accessoryQuickMoveOrder = this.accessoryEntries.stream()
                .filter(entry -> !entry.meta().cosmetic())
                .mapToInt(entry -> entry.slot().index)
                .toArray();
    }

    private void addAccessorySlots() {
        if (this.accessorySpecs.isEmpty()) {
            return;
        }
        AccessorySlotSource source = GearscopeServices.slotSource();
        // Positions only matter on the client; the server always uses unplaced (0/0) slots.
        AccessorySlotPlacer placer = this.player.level().isClientSide ? GearscopeServices.slotPlacer() : AccessorySlotPlacer.NONE;
        List<AccessoryPlacement> placements = placer.place(this.accessorySpecs);

        int placementIndex = 0;
        for (AccessorySlotSpec spec : this.accessorySpecs) {
            for (int i = 0; i < spec.size(); i++) {
                AccessoryPlacement placement = placementIndex < placements.size()
                        ? placements.get(placementIndex) : AccessoryPlacement.UNPLACED;
                placementIndex++;
                for (boolean cosmetic : new boolean[] {false, true}) {
                    AccessorySlotMeta meta = new AccessorySlotMeta(spec.slotName(), i, cosmetic, spec.groupName());
                    Slot slot = this.addSlot(source.createSlot(this.player, meta, placement.x(), placement.y()));
                    this.accessoryEntries.add(new AccessoryEntry(slot, meta, placement));
                }
            }
        }
    }

    public List<AccessorySlotSpec> accessorySpecs() {
        return this.accessorySpecs;
    }

    public List<AccessoryEntry> accessoryEntries() {
        return this.accessoryEntries;
    }

    /** The 4 vanilla armor slots (head, chest, legs, feet). */
    public List<Slot> armorSlots() {
        return this.armorSlots;
    }

    /** The 4 cosmetic armor slots (head, chest, legs, feet), or empty if the server did not enable them. */
    public List<Slot> cosmeticArmorSlots() {
        return this.cosmeticArmorSlots;
    }

    public boolean isAccessorySlot(int index) {
        return index >= ACCESSORY_SLOT_START && index < this.accessorySlotEnd;
    }

    /**
     * Sets the order in which shift-click tries the functional accessory slots. Ignored unless it is a
     * permutation of exactly those slot indices.
     */
    public void setAccessoryQuickMoveOrder(int[] order) {
        Set<Integer> expected = new HashSet<>();
        for (int index : this.accessoryQuickMoveOrder) {
            expected.add(index);
        }
        Set<Integer> given = new HashSet<>();
        for (int index : order) {
            if (!expected.contains(index) || !given.add(index)) {
                return;
            }
        }
        if (given.size() == expected.size()) {
            this.accessoryQuickMoveOrder = order.clone();
        }
    }

    /**
     * Stacks held by this menu outside the player inventory; they count as "used" for Accessories' hidden
     * unused slots so a slot does not vanish while its item is on the cursor or in the crafting grid.
     */
    public List<ItemStack> heldStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        if (!this.getCarried().isEmpty()) {
            stacks.add(this.getCarried());
        }
        for (ItemStack stack : this.craftSlots.getItems()) {
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    /** Removes and returns the crafting grid contents (used when the menu is reopened). */
    List<ItemStack> takeCraftingGrid() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < this.craftSlots.getContainerSize(); i++) {
            stacks.add(this.craftSlots.removeItemNoUpdate(i));
        }
        return stacks;
    }

    void restoreCraftingGrid(List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size() && i < this.craftSlots.getContainerSize(); i++) {
            this.craftSlots.setItem(i, stacks.get(i));
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
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        // Like vanilla, never pull from the crafting result; also never strip equipped accessories.
        return slot.container != this.resultSlots && !this.isAccessorySlot(slot.index) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(stackInSlot);
            boolean fromPlayerInventory = index >= INV_SLOT_START && index < HOTBAR_SLOT_END;

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
            } else if (index == OFFHAND_SLOT || this.isAccessorySlot(index)) {
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
            } else if (fromPlayerInventory && this.moveToAccessorySlot(stackInSlot)) {
                // Moved (at least partially) into the first free matching accessory slot.
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

    private boolean moveToAccessorySlot(ItemStack stack) {
        for (int index : this.accessoryQuickMoveOrder) {
            Slot target = this.slots.get(index);
            if (!target.hasItem() && target.mayPlace(stack) && this.moveItemStackTo(stack, index, index + 1, false)) {
                return true;
            }
        }
        return false;
    }
}
