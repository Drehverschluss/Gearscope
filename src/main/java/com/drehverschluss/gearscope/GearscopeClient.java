package com.drehverschluss.gearscope;

import com.drehverschluss.gearscope.client.GearscopeKeyMappings;
import com.drehverschluss.gearscope.client.GearscopeScreen;
import com.drehverschluss.gearscope.menu.GearscopeMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Gearscope.MODID, dist = Dist.CLIENT)
public class GearscopeClient {
    public GearscopeClient(IEventBus modEventBus) {
        modEventBus.addListener(GearscopeKeyMappings::register);
        modEventBus.addListener(GearscopeClient::onRegisterMenuScreens);
        NeoForge.EVENT_BUS.addListener(GearscopeKeyMappings::handleClientTick);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(GearscopeMenuTypes.CHARACTER.get(), GearscopeScreen::new);
    }
}
