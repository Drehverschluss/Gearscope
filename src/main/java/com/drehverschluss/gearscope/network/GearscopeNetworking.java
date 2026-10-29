package com.drehverschluss.gearscope.network;

import com.drehverschluss.gearscope.menu.GearscopeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration and server-side handling.
 */
public final class GearscopeNetworking {
    private static final String PROTOCOL_VERSION = "1";

    private GearscopeNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(OpenGearscopePayload.TYPE, OpenGearscopePayload.STREAM_CODEC,
                GearscopeNetworking::handleOpenGearscope);
    }

    private static void handleOpenGearscope(OpenGearscopePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, player) -> new GearscopeMenu(containerId, inventory),
                    Component.translatable("menu.gearscope.character")));
        }
    }
}
