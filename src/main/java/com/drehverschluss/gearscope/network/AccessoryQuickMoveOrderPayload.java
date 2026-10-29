package com.drehverschluss.gearscope.network;

import com.drehverschluss.gearscope.Gearscope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the order in which shift-click should try the functional accessory slots of the open
 * Gearscope menu (body places, then sidebars, then tabs). Only the client knows the layout, so it sends it.
 */
public record AccessoryQuickMoveOrderPayload(int containerId, int[] slotOrder) implements CustomPacketPayload {
    public static final Type<AccessoryQuickMoveOrderPayload> TYPE = new Type<>(Gearscope.id("accessory_quick_move_order"));

    private static final int MAX_SLOTS = 4096;

    public static final StreamCodec<RegistryFriendlyByteBuf, AccessoryQuickMoveOrderPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeVarInt(payload.slotOrder().length);
                for (int index : payload.slotOrder()) {
                    buf.writeVarInt(index);
                }
            },
            buf -> {
                int containerId = buf.readVarInt();
                int length = buf.readVarInt();
                if (length < 0 || length > MAX_SLOTS) {
                    throw new IllegalArgumentException("Invalid accessory slot order length: " + length);
                }
                int[] order = new int[length];
                for (int i = 0; i < length; i++) {
                    order[i] = buf.readVarInt();
                }
                return new AccessoryQuickMoveOrderPayload(containerId, order);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
