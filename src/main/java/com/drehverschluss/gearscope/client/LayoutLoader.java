package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.Gearscope;
import com.drehverschluss.gearscope.core.GearscopeLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads assets/gearscope/gearscope/layout.json (the topmost resource pack wins). Every missing entry keeps
 * its built-in default; invalid entries are reported in the log and replaced by the default as well. The
 * format is described in docs/layout.md.
 */
public final class LayoutLoader extends SimplePreparableReloadListener<GearscopeLayout> {
    public static final ResourceLocation LOCATION = Gearscope.id("gearscope/layout.json");

    @Override
    protected GearscopeLayout prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Optional<Resource> resource = resourceManager.getResource(LOCATION);
        if (resource.isEmpty()) {
            Gearscope.LOGGER.warn("{} not found; using the built-in layout", LOCATION);
            return GearscopeLayout.DEFAULT;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                Gearscope.LOGGER.warn("{} is not a JSON object; using the built-in layout", LOCATION);
                return GearscopeLayout.DEFAULT;
            }
            GearscopeLayout.Builder builder = new GearscopeLayout.Builder();
            new Reading(root.getAsJsonObject()).apply(builder);
            return builder.build();
        } catch (Exception e) {
            Gearscope.LOGGER.warn("Failed to read {}; using the built-in layout", LOCATION, e);
            return GearscopeLayout.DEFAULT;
        }
    }

    @Override
    protected void apply(GearscopeLayout layout, ResourceManager resourceManager, ProfilerFiller profiler) {
        GearscopeLayout.setCurrent(layout);
        // An open screen was built with the old positions; close it instead of mixing two layouts.
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.screen instanceof GearscopeScreen && minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        });
    }

    /** Reads one layout.json; each value falls back to the builder's default when missing or invalid. */
    private record Reading(JsonObject root) {
        void apply(GearscopeLayout.Builder b) {
            JsonObject screen = this.section("screen");
            readInt(screen, "screen", "width", v -> b.screenWidth = v, 1);
            readInt(screen, "screen", "height", v -> b.screenHeight = v, 1);

            JsonObject background = this.section("background");
            readLocation(background, "background", "texture", v -> b.backgroundTexture = v);
            readInt(background, "background", "texture_width", v -> b.backgroundTextureWidth = v, 1);
            readInt(background, "background", "texture_height", v -> b.backgroundTextureHeight = v, 1);

            JsonObject armor = this.section("armor");
            readInt(armor, "armor", "x", v -> b.armorX = v, 0);
            readInts(armor, "armor", "y", 4, v -> b.armorY = v);
            JsonObject offhand = this.section("offhand");
            readInt(offhand, "offhand", "x", v -> b.offhandX = v, 0);
            readInt(offhand, "offhand", "y", v -> b.offhandY = v, 0);

            JsonObject inventory = this.section("inventory");
            readInt(inventory, "inventory", "x", v -> b.inventoryX = v, 0);
            readInts(inventory, "inventory", "row_y", 3, v -> b.inventoryRowY = v);
            readInt(inventory, "inventory", "slot_step", v -> b.slotStep = v, 1);
            JsonObject hotbar = this.section("hotbar");
            readInt(hotbar, "hotbar", "y", v -> b.hotbarY = v, 0);

            JsonObject crafting = this.section("crafting");
            readInts(crafting, "crafting", "x", 2, v -> b.craftX = v);
            readInts(crafting, "crafting", "y", 2, v -> b.craftY = v);
            readInt(crafting, "crafting", "result_x", v -> b.resultX = v, 0);
            readInt(crafting, "crafting", "result_y", v -> b.resultY = v, 0);
            readInt(crafting, "crafting", "arrow_y", v -> b.craftArrowY = v, 0);

            JsonObject places = this.section("places");
            if (places != null) {
                for (Map.Entry<String, JsonElement> entry : places.entrySet()) {
                    int[] position = ints(entry.getValue(), 2);
                    if (position == null) {
                        warn("places." + entry.getKey(), "expected [x, y]");
                    } else {
                        // Known places move, new names are added (and can be used in slot_places.json).
                        b.places.put(entry.getKey(), position);
                    }
                }
            }

            JsonObject tabs = this.section("tabs");
            readInt(tabs, "tabs", "x", v -> b.tabX = v, 0);
            readInt(tabs, "tabs", "y", v -> b.tabY = v, 0);
            readInt(tabs, "tabs", "step", v -> b.tabStep = v, 1);
            readInt(tabs, "tabs", "width", v -> b.tabWidth = v, 1);
            readInt(tabs, "tabs", "height", v -> b.tabHeight = v, 1);
            readInt(tabs, "tabs", "visible", v -> b.tabsVisible = v, 1);
            readInt(tabs, "tabs", "scroll_up_y", v -> b.tabScrollUpY = v, 0);
            readInt(tabs, "tabs", "arrow_width", v -> b.arrowWidth = v, 1);
            readInt(tabs, "tabs", "arrow_height", v -> b.arrowHeight = v, 1);
            JsonObject tabSlots = this.section("tab_slots");
            readInts(tabSlots, "tab_slots", "x", -1, v -> b.tabSlotX = v);
            readInt(tabSlots, "tab_slots", "y", v -> b.tabSlotY = v, 0);
            readInt(tabSlots, "tab_slots", "step", v -> b.tabSlotStep = v, 1);
            readInt(tabSlots, "tab_slots", "rows", v -> b.tabSlotRows = v, 1);
            readInt(tabSlots, "tab_slots", "page_prev_x", v -> b.pagePrevX = v, 0);
            readInt(tabSlots, "tab_slots", "page_next_x", v -> b.pageNextX = v, 0);

            JsonObject cosmetic = this.section("cosmetic_button");
            readInt(cosmetic, "cosmetic_button", "x", v -> b.cosmeticButtonX = v, 0);
            readInt(cosmetic, "cosmetic_button", "y", v -> b.cosmeticButtonY = v, 0);
            readInt(cosmetic, "cosmetic_button", "size", v -> b.cosmeticButtonSize = v, 1);
            JsonObject recipeButton = this.section("recipe_button");
            readInt(recipeButton, "recipe_button", "x", v -> b.recipeButtonX = v, 0);
            readInt(recipeButton, "recipe_button", "y", v -> b.recipeButtonY = v, 0);
            readInt(recipeButton, "recipe_button", "size", v -> b.recipeButtonSize = v, 1);

            JsonObject player = this.section("player");
            readInt(player, "player", "center_x", v -> b.playerCenterX = v, 0);
            readInt(player, "player", "feet_y", v -> b.playerFeetY = v, 0);
            readInt(player, "player", "scale", v -> b.playerScale = v, 1);
            readInt(player, "player", "box_width", v -> b.playerBoxWidth = v, 1);
            readInt(player, "player", "box_height", v -> b.playerBoxHeight = v, 1);
            readFloat(player, "player", "y_offset", v -> b.playerYOffset = v);

            JsonObject attributes = this.section("attributes");
            readInt(attributes, "attributes", "x1", v -> b.scrollX1 = v, 0);
            readInt(attributes, "attributes", "y1", v -> b.scrollY1 = v, 0);
            readInt(attributes, "attributes", "x2", v -> b.scrollX2 = v, 0);
            readInt(attributes, "attributes", "y2", v -> b.scrollY2 = v, 0);
            readInt(attributes, "attributes", "list_y", v -> b.attrListY = v, 0);
            readInt(attributes, "attributes", "checkbox_size", v -> b.attrCheckboxSize = v, 1);
            readInt(attributes, "attributes", "scrollbar_width", v -> b.attrScrollbarWidth = v, 1);
            readInt(attributes, "attributes", "header_height", v -> b.attrHeaderHeight = v, 1);
            readInt(attributes, "attributes", "row_height", v -> b.attrRowHeight = v, 1);
            readInt(attributes, "attributes", "group_gap", v -> b.attrGroupGap = v, 0);
            readInt(attributes, "attributes", "icon_size", v -> b.attrIconSize = v, 1);
            readInt(attributes, "attributes", "tab_width", v -> b.attrTabWidth = v, 1);
            readInt(attributes, "attributes", "tab_height", v -> b.attrTabHeight = v, 1);
            readInt(attributes, "attributes", "tab_gap", v -> b.attrTabGap = v, 0);

            JsonObject effects = this.section("effects");
            readInt(effects, "effects", "tile_width", v -> b.effectTileWidth = v, 1);
            readInt(effects, "effects", "tile_height", v -> b.effectTileHeight = v, 1);
            readInt(effects, "effects", "tile_gap", v -> b.effectTileGap = v, 0);
            readInt(effects, "effects", "frame_gap", v -> b.effectFrameGap = v, 0);
            readInt(effects, "effects", "tile_icon_size", v -> b.effectTileIconSize = v, 1);
            readInt(effects, "effects", "row_height", v -> b.effectRowHeight = v, 1);
        }

        @Nullable
        private JsonObject section(String name) {
            JsonElement element = this.root.get(name);
            if (element == null) {
                return null;
            }
            if (!element.isJsonObject()) {
                warn(name, "expected an object");
                return null;
            }
            return element.getAsJsonObject();
        }

        private static void readInt(@Nullable JsonObject section, String sectionName, String key, Consumer<Integer> setter, int min) {
            JsonElement element = section != null ? section.get(key) : null;
            if (element == null) {
                return;
            }
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() && element.getAsInt() >= min) {
                setter.accept(element.getAsInt());
            } else {
                warn(sectionName + "." + key, "expected a whole number >= " + min);
            }
        }

        private static void readFloat(@Nullable JsonObject section, String sectionName, String key, Consumer<Float> setter) {
            JsonElement element = section != null ? section.get(key) : null;
            if (element == null) {
                return;
            }
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                setter.accept(element.getAsFloat());
            } else {
                warn(sectionName + "." + key, "expected a number");
            }
        }

        private static void readInts(@Nullable JsonObject section, String sectionName, String key, int length, Consumer<int[]> setter) {
            JsonElement element = section != null ? section.get(key) : null;
            if (element == null) {
                return;
            }
            int[] values = ints(element, length);
            if (values != null) {
                setter.accept(values);
            } else {
                warn(sectionName + "." + key, length > 0 ? "expected " + length + " whole numbers" : "expected a list of whole numbers");
            }
        }

        private static void readLocation(@Nullable JsonObject section, String sectionName, String key, Consumer<ResourceLocation> setter) {
            JsonElement element = section != null ? section.get(key) : null;
            if (element == null) {
                return;
            }
            ResourceLocation location = element.isJsonPrimitive() ? ResourceLocation.tryParse(element.getAsString()) : null;
            if (location != null) {
                setter.accept(location);
            } else {
                warn(sectionName + "." + key, "expected a resource location");
            }
        }

        /** The numbers of a JSON array, or null if it is not an array of exactly {@code length} (any if -1) numbers. */
        @Nullable
        private static int[] ints(JsonElement element, int length) {
            if (!element.isJsonArray()) {
                return null;
            }
            JsonArray array = element.getAsJsonArray();
            if ((length >= 0 && array.size() != length) || array.isEmpty()) {
                return null;
            }
            int[] values = new int[array.size()];
            for (int i = 0; i < values.length; i++) {
                JsonElement value = array.get(i);
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                    return null;
                }
                values[i] = value.getAsInt();
            }
            return values;
        }

        private static void warn(String entry, String problem) {
            Gearscope.LOGGER.warn("{}: invalid entry '{}' ({}); using the built-in default", LOCATION, entry, problem);
        }
    }
}
