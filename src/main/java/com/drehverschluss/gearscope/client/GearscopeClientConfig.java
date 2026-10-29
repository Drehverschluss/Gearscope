package com.drehverschluss.gearscope.client;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config (gearscope-client.toml). Values changed in the Gearscope screen are saved immediately.
 */
public final class GearscopeClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue AUTO_SCALE;
    public static final ModConfigSpec.BooleanValue HIDE_UNCHANGED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> COLLAPSED_GROUPS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HIDDEN_ATTRIBUTES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("The Gearscope screen").push("screen");
        AUTO_SCALE = builder
                .comment("Temporarily lower the GUI scale while Gearscope is open if the screen does not fit the window",
                        "at the current scale (the saved GUI scale option is never changed)")
                .define("autoScale", true);
        builder.pop();
        builder.comment("Attribute list on the Gearscope screen").push("attributes");
        HIDE_UNCHANGED = builder
                .comment("Only show attributes whose value differs from their base value")
                .define("hideUnchanged", false);
        COLLAPSED_GROUPS = builder
                .comment("Attribute groups that are collapsed (group ids, e.g. \"magic\")")
                .defineListAllowEmpty("collapsedGroups", List.of(), () -> "other", value -> value instanceof String);
        HIDDEN_ATTRIBUTES = builder
                .comment("Attributes that are never shown. Formats:",
                        "  minecraft:generic.luck  - a single attribute",
                        "  some_mod:*              - every attribute of a namespace",
                        "  !some_mod:visible_one   - shows an attribute again that a wildcard above hid",
                        "Attributes hidden by Apothic Attributes' \"Hidden Attributes\" config are hidden as well.")
                .defineListAllowEmpty("hiddenAttributes", List.of(), () -> "some_mod:*", value -> value instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    private GearscopeClientConfig() {
    }

    public static boolean isCollapsed(String groupId) {
        return COLLAPSED_GROUPS.get().contains(groupId);
    }

    public static void setCollapsed(String groupId, boolean collapsed) {
        List<String> groups = new ArrayList<>(COLLAPSED_GROUPS.get());
        groups.remove(groupId);
        if (collapsed) {
            groups.add(groupId);
        }
        COLLAPSED_GROUPS.set(groups);
        SPEC.save();
    }

    public static void setHideUnchanged(boolean hideUnchanged) {
        HIDE_UNCHANGED.set(hideUnchanged);
        SPEC.save();
    }
}
