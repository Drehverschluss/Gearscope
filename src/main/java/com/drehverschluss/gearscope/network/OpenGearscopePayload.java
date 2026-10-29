package com.drehverschluss.gearscope.network;

import com.drehverschluss.gearscope.Gearscope;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: "open the Gearscope menu". Carries no data, sent when the hotkey is pressed while no
 * screen is open.
 */
public record OpenGearscopePayload() implements CustomPacketPayload {
    public static final Type<OpenGearscopePayload> TYPE = new Type<>(Gearscope.id("open_gearscope"));

    public static final StreamCodec<ByteBuf, OpenGearscopePayload> STREAM_CODEC =
            StreamCodec.unit(new OpenGearscopePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
