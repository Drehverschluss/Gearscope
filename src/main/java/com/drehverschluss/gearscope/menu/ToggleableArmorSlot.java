package com.drehverschluss.gearscope.menu;

import com.drehverschluss.gearscope.core.ToggleableSlot;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ArmorSlot;

/**
 * Vanilla armor slot whose visibility the client screen can switch off (while cosmetic armor is shown at
 * the same position). The server never changes it, so it stays active there.
 */
class ToggleableArmorSlot extends ArmorSlot implements ToggleableSlot {
    private BooleanSupplier visibility = () -> true;

    ToggleableArmorSlot(Container container, LivingEntity owner, EquipmentSlot slot, int slotIndex, int x, int y,
            @Nullable ResourceLocation emptyIcon) {
        super(container, owner, slot, slotIndex, x, y, emptyIcon);
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
        return false;
    }

    @Override
    public List<Component> gearscope$emptySlotTooltip() {
        return List.of();
    }
}
