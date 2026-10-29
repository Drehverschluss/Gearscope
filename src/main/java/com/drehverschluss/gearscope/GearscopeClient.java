package com.drehverschluss.gearscope;

import com.drehverschluss.gearscope.client.GearscopeClientConfig;
import com.drehverschluss.gearscope.client.GearscopeKeyMappings;
import com.drehverschluss.gearscope.client.GearscopeScreen;
import com.drehverschluss.gearscope.client.GearscopeSlotPlacer;
import com.drehverschluss.gearscope.client.GuiScaleOverride;
import com.drehverschluss.gearscope.client.LayoutLoader;
import com.drehverschluss.gearscope.client.SlotPlacesLoader;
import com.drehverschluss.gearscope.client.attributes.AttributeGroupsLoader;
import com.drehverschluss.gearscope.compat.apothic.ApothicCompat;
import com.drehverschluss.gearscope.compat.polymorph.PolymorphCompat;
import com.drehverschluss.gearscope.compat.sophisticatedbackpacks.SophisticatedBackpacksCompat;
import com.drehverschluss.gearscope.core.GearscopeServices;
import com.drehverschluss.gearscope.menu.GearscopeMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Gearscope.MODID, dist = Dist.CLIENT)
public class GearscopeClient {
    public GearscopeClient(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, GearscopeClientConfig.SPEC);
        GearscopeServices.setSlotPlacer(new GearscopeSlotPlacer());

        // Apothic Attributes is optional; its classes are only touched when it is installed.
        if (ModList.get().isLoaded(ApothicCompat.MOD_ID)) {
            ApothicCompat.initClient();
        }
        // Same for the other optional client integrations (JEI finds its plugin through @JeiPlugin itself).
        if (ModList.get().isLoaded(PolymorphCompat.MOD_ID)) {
            PolymorphCompat.initClient(modEventBus);
        }
        if (ModList.get().isLoaded(SophisticatedBackpacksCompat.MOD_ID)) {
            SophisticatedBackpacksCompat.initClient();
        }

        modEventBus.addListener(GearscopeKeyMappings::register);
        modEventBus.addListener(GearscopeClient::onRegisterMenuScreens);
        modEventBus.addListener(GearscopeClient::onRegisterReloadListeners);
        NeoForge.EVENT_BUS.addListener(GearscopeKeyMappings::handleClientTick);
        NeoForge.EVENT_BUS.addListener(GuiScaleOverride::onClientTick);
        NeoForge.EVENT_BUS.addListener(GuiScaleOverride::onLoggingOut);
        // Before the screen and other mods (Mouse Tweaks remembers drag starts on mouse down): a click on an
        // accessory slot's render toggle must never reach the slot.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, GearscopeClient::onMousePressed);
    }

    private static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof GearscopeScreen screen
                && screen.clickRenderToggle(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(GearscopeMenuTypes.CHARACTER.get(), GearscopeScreen::new);
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new LayoutLoader());
        event.registerReloadListener(new SlotPlacesLoader());
        event.registerReloadListener(new AttributeGroupsLoader());
    }
}