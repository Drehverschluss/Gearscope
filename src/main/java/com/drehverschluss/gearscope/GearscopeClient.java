package com.drehverschluss.gearscope;

import com.drehverschluss.gearscope.client.GearscopeClientConfig;
import com.drehverschluss.gearscope.client.GearscopeKeyMappings;
import com.drehverschluss.gearscope.client.GearscopeScreen;
import com.drehverschluss.gearscope.client.GearscopeSlotPlacer;
import com.drehverschluss.gearscope.client.SlotPlacesLoader;
import com.drehverschluss.gearscope.client.attributes.AttributeGroupsLoader;
import com.drehverschluss.gearscope.compat.apothic.ApothicCompat;
import com.drehverschluss.gearscope.core.GearscopeServices;
import com.drehverschluss.gearscope.menu.GearscopeMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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

        modEventBus.addListener(GearscopeKeyMappings::register);
        modEventBus.addListener(GearscopeClient::onRegisterMenuScreens);
        modEventBus.addListener(GearscopeClient::onRegisterReloadListeners);
        NeoForge.EVENT_BUS.addListener(GearscopeKeyMappings::handleClientTick);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(GearscopeMenuTypes.CHARACTER.get(), GearscopeScreen::new);
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SlotPlacesLoader());
        event.registerReloadListener(new AttributeGroupsLoader());
    }
}