package com.drehverschluss.gearscope.compat.accessories;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.data.EntitySlotLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Like AccessoriesAPI.getUsedSlotsFor (registered items, inventory and equipped items), but wildcard items do
 * not count: items in accessories:any or valid for every (non-unique) slot type of the entity. Otherwise a
 * single such item (e.g. a Curios item tagged curios:curio) makes every slot type count as used.
 */
final class UsedSlotTypes {
    // Entity type -> slot type names that some non-wildcard registered item can go into. Cleared on tag reload.
    private static final Map<EntityType<?>, Set<String>> REGISTRY_CACHE = new ConcurrentHashMap<>();

    private UsedSlotTypes() {
    }

    static void clearCache() {
        REGISTRY_CACHE.clear();
    }

    static Set<String> withoutWildcards(Player player, Collection<ItemStack> extraUsedStacks) {
        Set<String> candidates = candidateNames(player);
        Set<String> names = new HashSet<>(REGISTRY_CACHE.computeIfAbsent(player.getType(), type -> scanRegistry(player, candidates)));
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            addValid(player, player.getInventory().getItem(i), candidates, names);
        }
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability != null) {
            for (SlotEntryReference reference : capability.getAllEquipped()) {
                addValid(player, reference.stack(), candidates, names);
            }
        }
        for (ItemStack stack : extraUsedStacks) {
            addValid(player, stack, candidates, names);
        }
        return names;
    }

    /** The slot types a wildcard item would fit into: all slot types of the entity except unique ones. */
    private static Set<String> candidateNames(Player player) {
        Set<String> names = new HashSet<>();
        for (SlotType slotType : EntitySlotLoader.getEntitySlots(player).values()) {
            if (!UniqueSlotHandling.isUniqueSlot(slotType.name())) {
                names.add(slotType.name());
            }
        }
        return names;
    }

    // Same per-item check as Accessories' own registry scan (slot validators only), so the cached result
    // does not depend on what the player currently wears.
    private static Set<String> scanRegistry(Player player, Set<String> candidates) {
        Set<String> names = new HashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            if (!stack.isEmpty()) {
                addUnlessWildcard(stack, AccessoriesAPI.getStackSlotTypes(player, stack), candidates, names);
            }
        }
        return Set.copyOf(names);
    }

    // Inventory and equipped stacks: the same check Accessories uses for them.
    private static void addValid(Player player, ItemStack stack, Set<String> candidates, Set<String> into) {
        if (!stack.isEmpty()) {
            addUnlessWildcard(stack, AccessoriesAPI.getValidSlotTypes(player, stack), candidates, into);
        }
    }

    private static void addUnlessWildcard(ItemStack stack, Collection<SlotType> validSlotTypes, Set<String> candidates, Set<String> into) {
        if (validSlotTypes.isEmpty() || stack.is(AccessoriesTags.ANY_TAG)) {
            return;
        }
        Set<String> valid = new HashSet<>();
        for (SlotType slotType : validSlotTypes) {
            valid.add(slotType.name());
        }
        if (candidates.size() > 1 && valid.containsAll(candidates)) {
            return;
        }
        into.addAll(valid);
    }
}
