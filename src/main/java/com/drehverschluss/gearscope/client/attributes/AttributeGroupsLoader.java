package com.drehverschluss.gearscope.client.attributes;

import com.drehverschluss.gearscope.Gearscope;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads and merges every assets/&lt;namespace&gt;/gearscope/attribute_groups.json from all namespaces and resource
 * packs. Files are applied from the lowest to the highest priority pack, so a resource pack can add or move
 * single attributes without copying the whole file. Format:
 * <pre>
 * { "groups": [ { "id": "offense", "order": 10, "attributes": ["minecraft:generic.attack_damage", "critical_strike:*"] } ] }
 * </pre>
 * "order" is optional when only adding attributes to an existing group.
 */
public final class AttributeGroupsLoader extends SimplePreparableReloadListener<AttributeGroups> {
    private static final String PATH = "gearscope/attribute_groups.json";

    private static volatile AttributeGroups groups = AttributeGroups.EMPTY;

    public static AttributeGroups groups() {
        return groups;
    }

    private record Source(ResourceLocation location, Resource resource, int packIndex) {
    }

    @Override
    protected AttributeGroups prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<String> packOrder = resourceManager.listPacks().map(pack -> pack.packId()).toList();
        List<Source> sources = new ArrayList<>();
        for (String namespace : resourceManager.getNamespaces()) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, PATH);
            for (Resource resource : resourceManager.getResourceStack(location)) {
                sources.add(new Source(location, resource, packOrder.indexOf(resource.sourcePackId())));
            }
        }
        sources.sort(Comparator.comparingInt(Source::packIndex));

        Map<String, Integer> groupOrder = new HashMap<>();
        // Entry ("ns:path" or "ns:*") -> group id. Reassigning removes and re-adds, moving it to the end.
        LinkedHashMap<String, String> assignments = new LinkedHashMap<>();
        for (Source source : sources) {
            try (Reader reader = source.resource().openAsReader()) {
                apply(JsonParser.parseReader(reader), groupOrder, assignments);
            } catch (Exception e) {
                Gearscope.LOGGER.error("Failed to read {} from pack {}", source.location(), source.resource().sourcePackId(), e);
            }
        }

        Map<ResourceLocation, String> exact = new HashMap<>();
        Map<ResourceLocation, Integer> exactIndex = new HashMap<>();
        Map<String, String> wildcards = new HashMap<>();
        Map<String, Integer> nextIndex = new HashMap<>();
        for (Map.Entry<String, String> entry : assignments.entrySet()) {
            String key = entry.getKey();
            String group = entry.getValue();
            if (key.endsWith(":*")) {
                wildcards.put(key.substring(0, key.length() - 2), group);
            } else {
                ResourceLocation id = ResourceLocation.parse(key);
                exact.put(id, group);
                exactIndex.put(id, nextIndex.merge(group, 1, Integer::sum) - 1);
            }
        }
        return new AttributeGroups(Map.copyOf(groupOrder), Map.copyOf(exact), Map.copyOf(exactIndex), Map.copyOf(wildcards));
    }

    @Override
    protected void apply(AttributeGroups loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        groups = loaded;
    }

    private static void apply(JsonElement root, Map<String, Integer> groupOrder, LinkedHashMap<String, String> assignments) {
        JsonArray groupsJson = root.getAsJsonObject().getAsJsonArray("groups");
        if (groupsJson == null) {
            return;
        }
        for (JsonElement element : groupsJson) {
            JsonObject group = element.getAsJsonObject();
            String id = group.get("id").getAsString();
            if (group.has("order")) {
                groupOrder.put(id, group.get("order").getAsInt());
            }
            JsonArray attributes = group.getAsJsonArray("attributes");
            if (attributes == null) {
                continue;
            }
            for (JsonElement attribute : attributes) {
                String key = attribute.getAsString();
                if (!isValidEntry(key)) {
                    Gearscope.LOGGER.warn("Ignoring invalid attribute entry '{}' in group '{}'", key, id);
                    continue;
                }
                assignments.remove(key);
                assignments.put(key, id);
            }
        }
    }

    private static boolean isValidEntry(String key) {
        if (key.endsWith(":*")) {
            return ResourceLocation.isValidNamespace(key.substring(0, key.length() - 2));
        }
        try {
            ResourceLocation.parse(key);
            return key.contains(":");
        } catch (ResourceLocationException e) {
            return false;
        }
    }
}
