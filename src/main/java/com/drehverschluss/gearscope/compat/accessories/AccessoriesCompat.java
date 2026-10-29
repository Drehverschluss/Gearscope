package com.drehverschluss.gearscope.compat.accessories;

import com.drehverschluss.gearscope.core.GearscopeServices;
import com.drehverschluss.gearscope.menu.GearscopeMenu;
import com.drehverschluss.gearscope.menu.GearscopeMenuOpener;
import io.wispforest.accessories.api.events.ContainersChangeCallback;
import net.minecraft.server.level.ServerPlayer;

/**
 * Entry point of the Accessories integration. Only called when Accessories is loaded, so no other part of
 * Gearscope ever loads an Accessories class.
 */
public final class AccessoriesCompat {
    public static final String MOD_ID = "accessories";

    private AccessoriesCompat() {
    }

    public static void init() {
        GearscopeServices.setSlotSource(new AccessoriesSlotSource());

        // Fired on the server after Accessories updated a container; the Boolean is true when its size changed.
        ContainersChangeCallback.EVENT.register((entity, capability, changedContainers) -> {
            if (entity instanceof ServerPlayer player
                    && player.containerMenu instanceof GearscopeMenu
                    && changedContainers.containsValue(Boolean.TRUE)) {
                GearscopeMenuOpener.markDirty(player);
            }
        });
    }
}
