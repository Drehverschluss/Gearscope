package com.drehverschluss.gearscope;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config (gearscope-common.toml). Read on the server, where the menu's accessory slots are built.
 */
public final class GearscopeCommonConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue IGNORE_WILDCARD_ITEMS_FOR_USED_SLOTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Accessories slots on the Gearscope screen").push("accessories");
        IGNORE_WILDCARD_ITEMS_FOR_USED_SLOTS = builder
                .comment("Like Accessories, Gearscope hides slot types that no item can go into (unless the player enabled",
                        "\"Show Unused Slots\" in Accessories). Items that fit into every slot type (accessories:any, e.g.",
                        "Curios items tagged curios:curio) make every slot type count as used. If true, such wildcard items",
                        "are ignored for this check, so slot types without a real item stay hidden. Slots that hold an item",
                        "are always shown. If false, Gearscope uses exactly Accessories' own check.")
                .define("ignoreWildcardItemsForUsedSlots", true);
        builder.pop();
        SPEC = builder.build();
    }

    private GearscopeCommonConfig() {
    }
}
