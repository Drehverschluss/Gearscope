package com.drehverschluss.gearscope.core;

import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/**
 * One Accessories slot type as it appears in a Gearscope menu: its name, how many slots it has and the
 * group it belongs to. The server builds the ordered list and sends it to the client when the menu opens,
 * so both sides create exactly the same slots in the same order.
 */
public record AccessorySlotSpec(String slotName, int size, String groupName) {
    /** Accessories' catch-all group; shown as Gearscope's "Other" tab. */
    public static final String UNSORTED_GROUP = "unsorted";

    private static final int MAX_SPECS = 1024;
    private static final int MAX_TOTAL_SLOTS = 2048;
    private static final int MAX_STRING_LENGTH = 256;

    /**
     * Server side: limits the list to what the network format accepts, so client and server always build
     * the same slots. Real setups stay far below these limits.
     */
    public static List<AccessorySlotSpec> capped(List<AccessorySlotSpec> specs) {
        List<AccessorySlotSpec> result = new ArrayList<>();
        int totalSlots = 0;
        for (AccessorySlotSpec spec : specs) {
            if (result.size() >= MAX_SPECS || totalSlots + spec.size() > MAX_TOTAL_SLOTS
                    || spec.slotName().length() > MAX_STRING_LENGTH || spec.groupName().length() > MAX_STRING_LENGTH) {
                break;
            }
            result.add(spec);
            totalSlots += spec.size();
        }
        return List.copyOf(result);
    }

    public static void writeList(FriendlyByteBuf buf, List<AccessorySlotSpec> specs) {
        buf.writeVarInt(specs.size());
        for (AccessorySlotSpec spec : specs) {
            buf.writeUtf(spec.slotName(), MAX_STRING_LENGTH);
            buf.writeVarInt(spec.size());
            buf.writeUtf(spec.groupName(), MAX_STRING_LENGTH);
        }
    }

    public static List<AccessorySlotSpec> readList(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_SPECS) {
            throw new DecoderException("Invalid accessory slot type count: " + count);
        }
        List<AccessorySlotSpec> specs = new ArrayList<>(count);
        int totalSlots = 0;
        for (int i = 0; i < count; i++) {
            String slotName = buf.readUtf(MAX_STRING_LENGTH);
            int size = buf.readVarInt();
            totalSlots += size;
            if (size < 0 || totalSlots > MAX_TOTAL_SLOTS) {
                throw new DecoderException("Invalid accessory slot size " + size + " for " + slotName);
            }
            specs.add(new AccessorySlotSpec(slotName, size, buf.readUtf(MAX_STRING_LENGTH)));
        }
        return List.copyOf(specs);
    }
}
