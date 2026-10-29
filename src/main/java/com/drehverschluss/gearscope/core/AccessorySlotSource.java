package com.drehverschluss.gearscope.core;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Everything Gearscope needs from an accessory mod. The rest of the mod only talks to this interface; the
 * Accessories implementation lives in compat/accessories and is only installed when Accessories is loaded.
 * Without it, {@link #EMPTY} provides no slots at all.
 */
public interface AccessorySlotSource {
    AccessorySlotSource EMPTY = new AccessorySlotSource() {
        @Override
        public List<AccessorySlotSpec> collectSpecs(Player player, Collection<ItemStack> extraUsedStacks) {
            return List.of();
        }

        @Override
        public boolean isOutdated(Player player, List<AccessorySlotSpec> specs, Collection<ItemStack> extraUsedStacks) {
            return false;
        }

        @Override
        public Slot createSlot(Player player, AccessorySlotMeta meta, int x, int y) {
            return placeholderSlot(x, y);
        }

        @Override
        public boolean supportsCosmeticArmor(Player player) {
            return false;
        }

        @Override
        public Slot createCosmeticArmorSlot(Player player, EquipmentSlot equipmentSlot, int x, int y) {
            return placeholderSlot(x, y);
        }

        @Override
        @Nullable
        public ResourceLocation groupIcon(Level level, String groupName) {
            return null;
        }

        @Override
        public String groupTranslationKey(Level level, String groupName) {
            return groupName;
        }
    };

    /**
     * Server side: the ordered list of slot types to show for this player. Must be deterministic.
     *
     * @param extraUsedStacks stacks outside the player inventory (carried item, crafting grid) that should
     *                        count as "used" when unused slots are hidden.
     */
    List<AccessorySlotSpec> collectSpecs(Player player, Collection<ItemStack> extraUsedStacks);

    /**
     * Server side: whether a menu built from {@code specs} no longer matches the player's containers (a size
     * changed or a new slot type became relevant) and must be reopened.
     */
    boolean isOutdated(Player player, List<AccessorySlotSpec> specs, Collection<ItemStack> extraUsedStacks);

    /**
     * Both sides: creates the menu slot for {@code meta}. Must always return a slot (a hidden placeholder if
     * the container is missing) so slot indices stay identical on client and server.
     */
    Slot createSlot(Player player, AccessorySlotMeta meta, int x, int y);

    /**
     * Server side: whether this player gets cosmetic armor slots (head, chest, legs, feet). The server sends
     * the answer to the client, so both sides add the same slots.
     */
    boolean supportsCosmeticArmor(Player player);

    /**
     * Both sides: the cosmetic slot for one armor piece; only changes how the player looks. Must always
     * return a slot (a hidden placeholder on failure) so slot indices stay aligned.
     */
    Slot createCosmeticArmorSlot(Player player, EquipmentSlot equipmentSlot, int x, int y);

    /**
     * Client side: the slot group's icon as a texture id without the "textures/" prefix and ".png" suffix
     * (e.g. accessories:gui/group/arm), or null for none.
     */
    @Nullable
    ResourceLocation groupIcon(Level level, String groupName);

    /**
     * Client side: translation key of a slot group's display name.
     */
    String groupTranslationKey(Level level, String groupName);

    /**
     * A slot that is never shown, never accepts items and holds nothing. Used to keep slot indices aligned
     * when a container cannot be resolved.
     */
    static Slot placeholderSlot(int x, int y) {
        return new PlaceholderSlot(x, y);
    }
}
