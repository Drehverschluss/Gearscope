package com.drehverschluss.gearscope.network;

import com.drehverschluss.gearscope.menu.GearscopeMenu;
import com.drehverschluss.gearscope.menu.GearscopeMenuOpener;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration and server-side handling.
 */
public final class GearscopeNetworking {
    private static final String PROTOCOL_VERSION = "2";

    private GearscopeNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(OpenGearscopePayload.TYPE, OpenGearscopePayload.STREAM_CODEC,
                GearscopeNetworking::handleOpenGearscope);
        registrar.playToServer(AccessoryQuickMoveOrderPayload.TYPE, AccessoryQuickMoveOrderPayload.STREAM_CODEC,
                GearscopeNetworking::handleAccessoryQuickMoveOrder);
    }

    private static void handleOpenGearscope(OpenGearscopePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            GearscopeMenuOpener.open(serverPlayer);
        }
    }

    private static void handleAccessoryQuickMoveOrder(AccessoryQuickMoveOrderPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof GearscopeMenu menu && menu.containerId == payload.containerId()) {
            menu.setAccessoryQuickMoveOrder(payload.slotOrder());
        }
    }
}
