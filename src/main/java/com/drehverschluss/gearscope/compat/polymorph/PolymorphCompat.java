package com.drehverschluss.gearscope.compat.polymorph;

import com.drehverschluss.gearscope.client.GearscopeScreen;
import com.drehverschluss.gearscope.menu.GearscopeMenu;
import com.illusivesoulworks.polymorph.api.client.PolymorphWidgets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Polymorph integration, client side. The server side needs nothing: the menu computes its result through
 * {@code CraftingMenu.slotChangedCraftingGrid}, which Polymorph already hooks.
 */
public final class PolymorphCompat {
    public static final String MOD_ID = "polymorph";

    private PolymorphCompat() {
    }

    public static void initClient(IEventBus modEventBus) {
        modEventBus.addListener(PolymorphCompat::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        PolymorphWidgets.getInstance().registerWidget(screen -> screen instanceof GearscopeScreen gearscope
                ? new GearscopeRecipesWidget(gearscope, gearscope.getMenu().getSlot(GearscopeMenu.RESULT_SLOT))
                : null);
    }
}
