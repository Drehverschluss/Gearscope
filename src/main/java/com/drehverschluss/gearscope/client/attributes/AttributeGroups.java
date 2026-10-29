package com.drehverschluss.gearscope.client.attributes;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;

/**
 * Merged attribute group configuration from all attribute_groups.json files.
 *
 * @param groupOrder   sort order of every known group id (lower first)
 * @param exact        exact attribute id -> group id
 * @param exactIndex   position of an exact attribute within its group's list (for sorting)
 * @param wildcards    namespace -> group id, from "namespace:*" entries
 */
public record AttributeGroups(Map<String, Integer> groupOrder, Map<ResourceLocation, String> exact,
        Map<ResourceLocation, Integer> exactIndex, Map<String, String> wildcards) {
    public static final String OTHER = "other";
    // Groups referenced without an order sort before "other" (which defaults to the very end).
    private static final int DEFAULT_ORDER = 500;
    private static final int OTHER_ORDER = 1000;

    public static final AttributeGroups EMPTY = new AttributeGroups(Map.of(), Map.of(), Map.of(), Map.of());

    /** Exact ids win over namespace wildcards; everything else goes to "other". */
    public String groupOf(ResourceLocation attribute) {
        String group = this.exact.get(attribute);
        if (group == null) {
            group = this.wildcards.get(attribute.getNamespace());
        }
        return group != null ? group : OTHER;
    }

    /** Position in the group's list if the attribute is listed exactly (and belongs to that group). */
    public OptionalInt exactPosition(ResourceLocation attribute) {
        return Optional.ofNullable(this.exactIndex.get(attribute)).map(OptionalInt::of).orElse(OptionalInt.empty());
    }

    public int order(String groupId) {
        Integer order = this.groupOrder.get(groupId);
        if (order != null) {
            return order;
        }
        return OTHER.equals(groupId) ? OTHER_ORDER : DEFAULT_ORDER;
    }
}
