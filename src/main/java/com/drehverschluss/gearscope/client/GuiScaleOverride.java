package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.Gearscope;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Temporarily lowers Minecraft's GUI scale while the Gearscope screen is open and does not fit the window
 * (e.g. GUI scale 4 in a 1080p window gives only 266 GUI pixels of height). Only the window's current scale
 * is changed, never the saved option; the scale Minecraft itself would use (same calculation as
 * Minecraft#resizeDisplay) is restored when the screen goes away.
 */
public final class GuiScaleOverride {
    private static boolean active;
    private static boolean warnedTooSmall;

    private GuiScaleOverride() {
    }

    /**
     * Called whenever the Gearscope screen is (re)initialized, also after window resizes: picks the largest
     * scale up to the player's own that fits {@code requiredWidth x requiredHeight} GUI pixels.
     */
    public static void apply(Minecraft minecraft, int requiredWidth, int requiredHeight) {
        Window window = minecraft.getWindow();
        int playerScale = playerScale(minecraft);
        int scale = playerScale;
        if (GearscopeClientConfig.AUTO_SCALE.get()) {
            while (scale > 1 && !fits(window, scale, requiredWidth, requiredHeight)) {
                scale--;
            }
        }
        if (!fits(window, scale, requiredWidth, requiredHeight) && !warnedTooSmall) {
            warnedTooSmall = true;
            Gearscope.LOGGER.warn("Gearscope screen ({}x{}) does not fit the window ({}x{} pixels) even at GUI scale {}{}; it is shown centered and cut off.",
                    requiredWidth, requiredHeight, window.getWidth(), window.getHeight(), scale,
                    GearscopeClientConfig.AUTO_SCALE.get() ? "" : " (autoScale is off)");
        }
        if (window.getGuiScale() != scale) {
            setScale(minecraft, scale);
        }
        active = scale != playerScale;
    }

    /** Restores Minecraft's own GUI scale; safe to call any time. */
    public static void restore(Minecraft minecraft) {
        if (!active) {
            return;
        }
        active = false;
        setScale(minecraft, playerScale(minecraft));
    }

    /** Safety net: never leave a lowered scale behind when the Gearscope screen is gone. */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (active && !(minecraft.screen instanceof GearscopeScreen)) {
            restore(minecraft);
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        restore(Minecraft.getInstance());
    }

    private static int playerScale(Minecraft minecraft) {
        return minecraft.getWindow().calculateScale(minecraft.options.guiScale().get(), minecraft.isEnforceUnicode());
    }

    private static void setScale(Minecraft minecraft, int scale) {
        Window window = minecraft.getWindow();
        window.setGuiScale(scale);
        ClientHooks.resizeGuiLayers(minecraft, window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    /** Same rounding as Window#setGuiScale. */
    private static boolean fits(Window window, int scale, int requiredWidth, int requiredHeight) {
        return guiSize(window.getWidth(), scale) >= requiredWidth && guiSize(window.getHeight(), scale) >= requiredHeight;
    }

    private static int guiSize(int framebufferSize, int scale) {
        int size = framebufferSize / scale;
        return (double) framebufferSize / scale > size ? size + 1 : size;
    }
}
