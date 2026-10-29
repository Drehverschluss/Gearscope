package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.network.OpenGearscopePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Gearscope hotkey: unbound by default (like Perilscope). Opens the menu when pressed with no screen
 * open. Inside screens key presses do not reach key mappings, so closing is handled on key press: the
 * Gearscope screen always closes with it ({@link GearscopeScreen#keyPressed}), other container screens only
 * with the client option closeContainersWithKey ({@link #onScreenKeyPressed}).
 */
public final class GearscopeKeyMappings {
    private static final String CATEGORY = "key.categories.gearscope";

    public static final KeyMapping OPEN_GEARSCOPE =
            new KeyMapping("key.gearscope.open", InputConstants.UNKNOWN.getValue(), CATEGORY);

    private GearscopeKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GEARSCOPE);
    }

    /** Whether the key press is the Gearscope key, including its key modifier (e.g. Shift). */
    static boolean isGearscopeKey(int keyCode, int scanCode) {
        return OPEN_GEARSCOPE.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
    }

    /**
     * Optionally closes other container screens with the Gearscope key, like the vanilla inventory key does
     * (useful when Gearscope is bound to E and the vanilla inventory to another key). Registered with low
     * priority, so JEI's search field (which cancels the event) and other handlers come first; never while a
     * text field has focus and never in the creative inventory (its search tab takes text).
     */
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!GearscopeClientConfig.CLOSE_CONTAINERS_WITH_KEY.get()
                || !(screen instanceof AbstractContainerScreen<?>)
                || screen instanceof GearscopeScreen
                || screen instanceof CreativeModeInventoryScreen
                || !isGearscopeKey(event.getKeyCode(), event.getScanCode())
                || TextInputFocus.isTextInputFocused(screen)) {
            return;
        }
        screen.onClose();
        event.setCanceled(true);
    }

    public static void handleClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_GEARSCOPE.consumeClick()) {
            if (minecraft.screen instanceof GearscopeScreen) {
                if (minecraft.player != null) {
                    minecraft.player.closeContainer();
                }
            } else if (minecraft.screen == null) {
                PacketDistributor.sendToServer(new OpenGearscopePayload());
            }
        }
    }
}
