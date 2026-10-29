package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.network.OpenGearscopePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Gearscope hotkey: unbound by default (like Perilscope). Opens the menu when pressed with no screen
 * open, and closes it again (like the vanilla inventory key) when Gearscope is already open.
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
