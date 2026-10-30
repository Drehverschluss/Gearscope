package com.drehverschluss.gearscope.compat.quark;

import com.drehverschluss.gearscope.client.GearscopeReturn;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.violetmoon.quark.content.management.inventory.HeldShulkerBoxMenu;

/** Quark integration, client side: a shulker box opened from Gearscope returns to Gearscope when closed. */
public final class QuarkClientCompat {
    private QuarkClientCompat() {
    }

    public static void initClient() {
        GearscopeReturn.registerChildScreens(screen -> screen instanceof AbstractContainerScreen<?> containerScreen
                && containerScreen.getMenu() instanceof HeldShulkerBoxMenu);
    }
}
