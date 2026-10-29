package com.drehverschluss.gearscope;

import com.drehverschluss.gearscope.menu.GearscopeMenuTypes;
import com.drehverschluss.gearscope.network.GearscopeNetworking;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Gearscope.MODID)
public class Gearscope {
    public static final String MODID = "gearscope";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Gearscope(IEventBus modEventBus, ModContainer modContainer) {
        GearscopeMenuTypes.MENU_TYPES.register(modEventBus);
        modEventBus.addListener(GearscopeNetworking::registerPayloads);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
