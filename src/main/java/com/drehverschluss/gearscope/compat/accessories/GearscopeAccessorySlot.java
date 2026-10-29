package com.drehverschluss.gearscope.compat.accessories;

import com.drehverschluss.gearscope.core.ToggleableSlot;
import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.slot.SlotType;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

/**
 * An Accessories slot (functional or cosmetic) at a free position. Cosmetic handling mirrors Accessories'
 * own internal slot: cosmetic slots accept anything the slot type's validators accept and can always be
 * emptied. Visibility is controlled by the Gearscope screen on the client only.
 */
public class GearscopeAccessorySlot extends AccessoriesBasedSlot implements ToggleableSlot {
    private final boolean cosmetic;
    @Nullable
    private final ResourceLocation emptyIcon;
    private BooleanSupplier visibility = () -> true;

    public GearscopeAccessorySlot(AccessoriesContainer container, boolean cosmetic, int index, int x, int y) {
        this(container, cosmetic, index, x, y, null);
    }

    /**
     * @param emptyIcon block-atlas sprite shown while empty instead of the slot type icon (used for the
     *                  cosmetic armor slots, which show the vanilla armor outlines like Accessories does)
     */
    public GearscopeAccessorySlot(AccessoriesContainer container, boolean cosmetic, int index, int x, int y,
            @Nullable ResourceLocation emptyIcon) {
        super(container, cosmetic ? container.getCosmeticAccessories() : container.getAccessories(), index, x, y);
        this.cosmetic = cosmetic;
        this.emptyIcon = emptyIcon;
    }

    @Override
    @Nullable
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return this.emptyIcon != null ? Pair.of(InventoryMenu.BLOCK_ATLAS, this.emptyIcon) : super.getNoItemIcon();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!this.cosmetic) {
            return super.mayPlace(stack);
        }
        SlotType slotType = this.accessoriesContainer.slotType();
        return slotType != null && AccessoriesAPI.getPredicateResults(
                slotType.validators(), this.entity.level(), this.entity, slotType, 0, stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.cosmetic || super.mayPickup(player);
    }

    @Override
    public boolean isCosmeticSlot() {
        return this.cosmetic;
    }

    @Override
    public boolean isActive() {
        return this.visibility.getAsBoolean() && super.isActive();
    }

    @Override
    public void gearscope$setVisibility(BooleanSupplier visibility) {
        this.visibility = visibility;
    }

    @Override
    public boolean gearscope$isCosmetic() {
        return this.cosmetic;
    }

    @Override
    public List<Component> gearscope$emptySlotTooltip() {
        SlotType slotType = this.accessoriesContainer.slotType();
        if (slotType == null) {
            return List.of();
        }
        String key = this.cosmetic ? "accessories.cosmetic_slot.tooltip.singular" : "accessories.slot.tooltip.singular";
        return List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(slotType.translation()).withStyle(ChatFormatting.BLUE)));
    }
}
