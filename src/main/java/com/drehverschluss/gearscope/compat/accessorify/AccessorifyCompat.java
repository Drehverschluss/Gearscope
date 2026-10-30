package com.drehverschluss.gearscope.compat.accessorify;

import com.drehverschluss.gearscope.Gearscope;
import com.drehverschluss.gearscope.client.GearscopeReturn;
import com.drehverschluss.gearscope.client.GearscopeScreen;
import com.drehverschluss.gearscope.core.GearscopeServices;
import com.drehverschluss.gearscope.menu.GearscopeMenu;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.pajic.accessorify.network.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Accessorify integration, client side only. Accessorify opens a shulker box from its "shulker" accessory slot and
 * the ender chest from its accessory slot through its selection widget and key bindings. In Gearscope the same
 * happens with the click Quark uses for its in-place opening (right click by default) and an empty cursor, by
 * sending Accessorify's own packets with the values Accessorify itself would send. Left and shift clicks stay
 * normal (taking the item out), cosmetic slots never open anything.
 */
public final class AccessorifyCompat {
    public static final String MOD_ID = "accessorify";
    private static final String SHULKER_SLOT = "shulker";
    private static final Map<String, Boolean> WARNED = new ConcurrentHashMap<>();

    private AccessorifyCompat() {
    }

    public static void initClient() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, AccessorifyCompat::onMouseButtonPressed);
    }

    private static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof GearscopeScreen screen)
                || event.getButton() != GearscopeServices.openItemButton().getAsInt()
                || Screen.hasShiftDown()
                || !screen.getMenu().getCarried().isEmpty()) {
            return;
        }
        Slot slot = screen.getSlotUnderMouse();
        GearscopeMenu.AccessoryEntry entry = slot == null ? null : entryOf(screen.getMenu(), slot);
        if (entry == null || entry.meta().cosmetic() || !slot.hasItem()) {
            return;
        }
        ItemStack stack = slot.getItem();
        Minecraft minecraft = Minecraft.getInstance();
        if (SHULKER_SLOT.equals(entry.meta().slotName()) && setting("shulkerBoxAccessory")) {
            // Accessorify's index is the slot index inside its "shulker" accessories container.
            GearscopeReturn.expect(next -> next instanceof ShulkerBoxScreen);
            PacketDistributor.sendToServer(new Payloads.C2SOpenShulkerBoxPayload(entry.meta().index()));
            if (minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.SHULKER_BOX_OPEN);
            }
            event.setCanceled(true);
        } else if (stack.is(Items.ENDER_CHEST) && setting("enderChestAccessory")) {
            // Accessorify's own ender chest slot ("ender_chest") or wherever its slot mode puts it (e.g. "back").
            GearscopeReturn.expect(next -> next instanceof ContainerScreen);
            PacketDistributor.sendToServer(new Payloads.C2SOpenEnderContainerPayload());
            if (minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.ENDER_CHEST_OPEN);
            }
            event.setCanceled(true);
        }
    }

    private static GearscopeMenu.AccessoryEntry entryOf(GearscopeMenu menu, Slot slot) {
        for (GearscopeMenu.AccessoryEntry entry : menu.accessoryEntries()) {
            if (entry.slot() == slot) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Accessorify's accessorySettings switch (Accessorify.CONFIG.accessorySettings.&lt;name&gt;.get()). Read
     * reflectively because the value types belong to Fzzy Config; if that fails the feature stays on, like
     * Accessorify's own default.
     */
    private static boolean setting(String name) {
        try {
            Object config = Class.forName("me.pajic.accessorify.Accessorify").getField("CONFIG").get(null);
            Object settings = field(config, "accessorySettings");
            Object value = field(settings, name);
            Method get = value.getClass().getMethod("get");
            return !Boolean.FALSE.equals(get.invoke(value));
        } catch (ReflectiveOperationException | RuntimeException error) {
            if (WARNED.putIfAbsent(name, Boolean.TRUE) == null) {
                Gearscope.LOGGER.warn("Gearscope: could not read Accessorify's {} setting, assuming it is on", name, error);
            }
            return true;
        }
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getField(name);
        return field.get(owner);
    }
}
