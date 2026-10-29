package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.Gearscope;
import com.drehverschluss.gearscope.core.GearscopeLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads assets/gearscope/gearscope/slot_places.json: which named place(s) each Accessories slot type is
 * drawn at. A resource pack can override the file. Format:
 * <pre>{ "slot_places": { "ring": ["body_left_mid", "body_right_mid"], "cape": "body_right_top" } }</pre>
 * The n-th slot of a type takes the n-th free place of its list; slots without a free place go to the tabs.
 */
public final class SlotPlacesLoader extends SimplePreparableReloadListener<Map<String, List<String>>> {
    public static final ResourceLocation LOCATION = Gearscope.id("gearscope/slot_places.json");

    private static volatile Map<String, List<String>> slotPlaces = Map.of();

    public static Map<String, List<String>> slotPlaces() {
        return slotPlaces;
    }

    @Override
    protected Map<String, List<String>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Optional<Resource> resource = resourceManager.getResource(LOCATION);
        if (resource.isEmpty()) {
            Gearscope.LOGGER.warn("{} not found; all accessory slots will be shown in the tabs", LOCATION);
            return Map.of();
        }
        try (Reader reader = resource.get().openAsReader()) {
            return parse(JsonParser.parseReader(reader));
        } catch (Exception e) {
            Gearscope.LOGGER.error("Failed to read {}; all accessory slots will be shown in the tabs", LOCATION, e);
            return Map.of();
        }
    }

    @Override
    protected void apply(Map<String, List<String>> places, ResourceManager resourceManager, ProfilerFiller profiler) {
        slotPlaces = places;
    }

    private static Map<String, List<String>> parse(JsonElement root) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        JsonObject slotPlacesJson = root.getAsJsonObject().getAsJsonObject("slot_places");
        if (slotPlacesJson == null) {
            Gearscope.LOGGER.warn("{} has no \"slot_places\" object", LOCATION);
            return Map.of();
        }
        for (Map.Entry<String, JsonElement> entry : slotPlacesJson.entrySet()) {
            List<String> places = new ArrayList<>();
            JsonArray array = new JsonArray();
            if (entry.getValue().isJsonArray()) {
                array = entry.getValue().getAsJsonArray();
            } else {
                array.add(entry.getValue());
            }
            for (JsonElement element : array) {
                String place = element.getAsString();
                if (GearscopeLayout.place(place) == null) {
                    Gearscope.LOGGER.warn("{}: unknown place '{}' for slot type '{}'", LOCATION, place, entry.getKey());
                } else {
                    places.add(place);
                }
            }
            result.put(entry.getKey(), List.copyOf(places));
        }
        return Map.copyOf(result);
    }
}
