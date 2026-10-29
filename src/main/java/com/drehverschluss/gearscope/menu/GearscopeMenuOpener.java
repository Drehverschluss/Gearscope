package com.drehverschluss.gearscope.menu;

import com.drehverschluss.gearscope.Gearscope;
import com.drehverschluss.gearscope.core.AccessorySlotSpec;
import com.drehverschluss.gearscope.core.GearscopeServices;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server side opening of the Gearscope menu, and reopening it when the player's accessory slots change
 * while it is open (Accessories only reopens its own menu, so Gearscope has to do this itself).
 */
public final class GearscopeMenuOpener {
    private static final Component TITLE = Component.translatable("menu.gearscope.character");
    // Also poll every few ticks, so slots that become "used" (new item picked up) appear without reopening.
    private static final int POLL_INTERVAL_TICKS = 10;
    private static final Set<UUID> DIRTY_PLAYERS = ConcurrentHashMap.newKeySet();

    private GearscopeMenuOpener() {
    }

    public static void open(ServerPlayer player) {
        List<AccessorySlotSpec> specs = AccessorySlotSpec.capped(GearscopeServices.slotSource().collectSpecs(player, List.of()));
        openMenu(player, specs, false);
    }

    private static void openMenu(ServerPlayer player, List<AccessorySlotSpec> specs, boolean reopen) {
        boolean cosmeticArmor = GearscopeServices.slotSource().supportsCosmeticArmor(player);
        player.openMenu(new Provider(specs, cosmeticArmor, reopen), buf -> {
            AccessorySlotSpec.writeList(buf, specs);
            buf.writeBoolean(cosmeticArmor);
        });
    }

    /**
     * Marks a player whose accessory containers changed; the menu is checked (and reopened if needed) at
     * the end of the server tick, after Accessories has synced the new container sizes to the client.
     */
    public static void markDirty(Player player) {
        DIRTY_PLAYERS.add(player.getUUID());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        boolean poll = event.getServer().getTickCount() % POLL_INTERVAL_TICKS == 0;
        if (!poll && DIRTY_PLAYERS.isEmpty()) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            boolean dirty = DIRTY_PLAYERS.remove(player.getUUID());
            if ((dirty || poll) && player.containerMenu instanceof GearscopeMenu menu
                    && GearscopeServices.slotSource().isOutdated(player, menu.accessorySpecs(), menu.heldStacks())) {
                reopen(player, menu);
            }
        }
        DIRTY_PLAYERS.clear();
    }

    private static void reopen(ServerPlayer player, GearscopeMenu oldMenu) {
        // Take the cursor item and crafting grid out of the old menu so closing it does not move them into
        // the inventory, then hand them to the new menu. Nothing is copied, so nothing can be duplicated.
        ItemStack carried = oldMenu.getCarried();
        oldMenu.setCarried(ItemStack.EMPTY);
        List<ItemStack> craftingGrid = oldMenu.takeCraftingGrid();

        List<AccessorySlotSpec> specs = AccessorySlotSpec.capped(
                GearscopeServices.slotSource().collectSpecs(player, heldStacks(carried, craftingGrid)));
        openMenu(player, specs, true);

        if (player.containerMenu instanceof GearscopeMenu newMenu && newMenu != oldMenu) {
            newMenu.restoreCraftingGrid(craftingGrid);
            newMenu.setCarried(carried);
            newMenu.broadcastChanges();
        } else {
            Gearscope.LOGGER.warn("Could not reopen the Gearscope menu for {}; returning held items to the inventory", player.getGameProfile().getName());
            for (ItemStack stack : craftingGrid) {
                player.getInventory().placeItemBackInInventory(stack);
            }
            player.getInventory().placeItemBackInInventory(carried);
        }
    }

    private static List<ItemStack> heldStacks(ItemStack carried, List<ItemStack> craftingGrid) {
        List<ItemStack> stacks = new ArrayList<>(craftingGrid);
        stacks.add(carried);
        stacks.removeIf(ItemStack::isEmpty);
        return stacks;
    }

    private record Provider(List<AccessorySlotSpec> specs, boolean cosmeticArmor, boolean reopen) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return TITLE;
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new GearscopeMenu(containerId, playerInventory, this.specs, this.cosmeticArmor);
        }

        @Override
        public boolean shouldTriggerClientSideContainerClosingOnOpen() {
            // When reopening, switch screens directly instead of closing first (no flicker, cursor stays put).
            return !this.reopen;
        }
    }
}
