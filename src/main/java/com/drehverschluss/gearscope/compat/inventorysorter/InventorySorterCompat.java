package com.drehverschluss.gearscope.compat.inventorysorter;

import com.drehverschluss.gearscope.core.PlaceholderSlot;
import com.drehverschluss.gearscope.menu.GearscopeCraftingSlot;
import com.drehverschluss.gearscope.menu.GearscopeOffhandSlot;
import com.drehverschluss.gearscope.menu.GearscopeResultSlot;
import com.drehverschluss.gearscope.menu.ToggleableArmorSlot;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Inventory Sorter integration: keeps sorting and wheel-moving to the inventory and hotbar. Inventory
 * Sorter matches its slot blacklist by exact class name ({@code slot.getClass().getName()}), so only
 * Gearscope's own slot classes are listed here, never vanilla or Accessories classes, which would also
 * affect other screens.
 */
public final class InventorySorterCompat {
    public static final String MOD_ID = "inventorysorter";

    private static final List<String> BLACKLISTED_SLOT_CLASSES = List.of(
            GearscopeResultSlot.class.getName(),
            GearscopeCraftingSlot.class.getName(),
            ToggleableArmorSlot.class.getName(),
            GearscopeOffhandSlot.class.getName(),
            PlaceholderSlot.class.getName(),
            // As a string: referencing the class would load Accessories classes, which may be missing.
            "com.drehverschluss.gearscope.compat.accessories.GearscopeAccessorySlot");

    private InventorySorterCompat() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(InventorySorterCompat::onEnqueueImc);
    }

    private static void onEnqueueImc(InterModEnqueueEvent event) {
        for (String className : BLACKLISTED_SLOT_CLASSES) {
            InterModComms.sendTo(MOD_ID, "slotblacklist", () -> className);
        }
    }
}
