package com.drehverschluss.gearscope.compat.accessories;

import com.drehverschluss.gearscope.Gearscope;
import com.drehverschluss.gearscope.core.AccessorySlotMeta;
import com.drehverschluss.gearscope.core.AccessorySlotSource;
import com.drehverschluss.gearscope.core.AccessorySlotSpec;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.menu.ArmorSlotTypes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Reads the player's Accessories slots at runtime. Which slot types are shown follows Accessories' own
 * screen: the entity's valid groups without unique groups, containers of size 0 skipped, and, unless the
 * player enabled "show unused slots", only slot types that an item the player has could go into
 * (AccessoriesAPI.getUsedSlotsFor).
 */
final class AccessoriesSlotSource implements AccessorySlotSource {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    // Tabs follow the group order like Accessories (higher order first); "unsorted" always goes last.
    private static final Comparator<SlotGroup> GROUP_ORDER = Comparator
            .comparing((SlotGroup group) -> AccessorySlotSpec.UNSORTED_GROUP.equals(group.name()))
            .thenComparing(Comparator.comparingInt(SlotGroup::order).reversed())
            .thenComparing(SlotGroup::name);
    private static final Comparator<SlotType> SLOT_ORDER = Comparator
            .comparingInt(SlotType::order).reversed()
            .thenComparing(SlotType::name);

    private volatile boolean cosmeticArmorDisabled;

    @Override
    public List<AccessorySlotSpec> collectSpecs(Player player, Collection<ItemStack> extraUsedStacks) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            return List.of();
        }
        Map<String, AccessoriesContainer> containers = capability.getContainers();
        boolean showUnused = AccessoriesHolder.getOptionally(player).map(AccessoriesHolder::showUnusedSlots).orElse(false);
        Set<String> usedSlotNames = showUnused ? null : usedSlotNames(player, extraUsedStacks);

        List<Map.Entry<SlotGroup, List<SlotType>>> groups = new ArrayList<>(SlotGroupLoader.getValidGroups(player).entrySet());
        groups.sort(Map.Entry.comparingByKey(GROUP_ORDER));

        List<AccessorySlotSpec> specs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<SlotGroup, List<SlotType>> group : groups) {
            List<SlotType> slotTypes = new ArrayList<>(group.getValue());
            slotTypes.sort(SLOT_ORDER);
            for (SlotType slotType : slotTypes) {
                String name = slotType.name();
                if (!seen.add(name) || UniqueSlotHandling.isUniqueSlot(name)) {
                    continue;
                }
                AccessoriesContainer container = containers.get(name);
                if (container == null || container.slotType() == null || container.getSize() <= 0) {
                    continue;
                }
                if (usedSlotNames != null && !usedSlotNames.contains(name)) {
                    continue;
                }
                specs.add(new AccessorySlotSpec(name, container.getSize(), group.getKey().name()));
            }
        }
        return specs;
    }

    @Override
    public boolean isOutdated(Player player, List<AccessorySlotSpec> specs, Collection<ItemStack> extraUsedStacks) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            return !specs.isEmpty();
        }
        Map<String, AccessoriesContainer> containers = capability.getContainers();
        for (AccessorySlotSpec spec : specs) {
            AccessoriesContainer container = containers.get(spec.slotName());
            if (container == null || container.getSize() != spec.size()) {
                return true;
            }
        }
        // Slot types that disappear (unused) stay until the next reopen; only new ones force a reopen.
        Set<AccessorySlotSpec> current = new HashSet<>(specs);
        for (AccessorySlotSpec spec : AccessorySlotSpec.capped(this.collectSpecs(player, extraUsedStacks))) {
            if (!current.contains(spec)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Slot createSlot(Player player, AccessorySlotMeta meta, int x, int y) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        AccessoriesContainer container = capability != null ? capability.getContainers().get(meta.slotName()) : null;
        if (container == null || container.slotType() == null) {
            return AccessorySlotSource.placeholderSlot(x, y);
        }
        return new GearscopeAccessorySlot(container, meta.cosmetic(), meta.index(), x, y);
    }

    @Override
    public boolean supportsCosmeticArmor(Player player) {
        if (this.cosmeticArmorDisabled) {
            return false;
        }
        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            if (this.cosmeticArmorContainer(player, equipmentSlot) == null) {
                return false;
            }
        }
        return !this.cosmeticArmorDisabled;
    }

    @Override
    public Slot createCosmeticArmorSlot(Player player, EquipmentSlot equipmentSlot, int x, int y) {
        AccessoriesContainer container = this.cosmeticArmorContainer(player, equipmentSlot);
        if (container == null) {
            return AccessorySlotSource.placeholderSlot(x, y);
        }
        try {
            // Same as Accessories' own menu: cosmetic slot on the armor slot type's container, showing the
            // vanilla empty armor outline.
            ResourceLocation emptyIcon = ArmorSlotTypes.TEXTURE_EMPTY_SLOTS.get(equipmentSlot);
            return new GearscopeAccessorySlot(container, true, 0, x, y, emptyIcon);
        } catch (RuntimeException | LinkageError e) {
            this.disableCosmeticArmor(e);
            return AccessorySlotSource.placeholderSlot(x, y);
        }
    }

    /**
     * The container of Accessories' hidden armor slot type for this armor piece. ArmorSlotTypes is not part of
     * Accessories' API, so any failure disables cosmetic armor (logged once) instead of crashing.
     */
    @Nullable
    private AccessoriesContainer cosmeticArmorContainer(Player player, EquipmentSlot equipmentSlot) {
        if (this.cosmeticArmorDisabled) {
            return null;
        }
        try {
            SlotTypeReference reference = ArmorSlotTypes.getReferenceFromSlot(equipmentSlot);
            AccessoriesCapability capability = AccessoriesCapability.get(player);
            if (reference == null || capability == null) {
                return null;
            }
            AccessoriesContainer container = capability.getContainer(reference);
            return container != null && container.slotType() != null && container.getSize() > 0 ? container : null;
        } catch (RuntimeException | LinkageError e) {
            this.disableCosmeticArmor(e);
            return null;
        }
    }

    private void disableCosmeticArmor(Throwable error) {
        if (!this.cosmeticArmorDisabled) {
            this.cosmeticArmorDisabled = true;
            Gearscope.LOGGER.error("Gearscope: cosmetic armor slots disabled, Accessories' armor slot types are not usable", error);
        }
    }

    @Override
    @Nullable
    public ResourceLocation groupIcon(Level level, String groupName) {
        return SlotGroupLoader.getGroup(level, groupName).map(SlotGroup::icon).orElse(SlotGroup.UNKNOWN);
    }

    @Override
    public String groupTranslationKey(Level level, String groupName) {
        return SlotGroupLoader.getGroup(level, groupName).map(SlotGroup::translation).orElse(groupName);
    }

    private static Set<String> usedSlotNames(Player player, Collection<ItemStack> extraUsedStacks) {
        Set<String> names = new HashSet<>();
        for (SlotType slotType : AccessoriesAPI.getUsedSlotsFor(player, player.getInventory())) {
            names.add(slotType.name());
        }
        for (ItemStack stack : extraUsedStacks) {
            for (SlotType slotType : AccessoriesAPI.getValidSlotTypes(player, stack)) {
                names.add(slotType.name());
            }
        }
        return names;
    }
}
