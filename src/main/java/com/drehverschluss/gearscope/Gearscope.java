package com.drehverschluss.gearscope;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod("gearscope")
public class Gearscope {
    public static final String MODID = "gearscope";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Gearscope(IEventBus modEventBus, ModContainer modContainer) {
    }
}
