package com.drehverschluss.gearscope.client;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

/** Where the Gearscope screen shows the player's active effects (client config [effects] display). */
public enum EffectDisplayMode implements TranslatableEnum {
    /** Row of effect tiles above the Gearscope frame; SCROLL_TAB if there is not enough room above it. */
    TOP,
    /** An "Effects" group at the top of the attribute scroll. */
    SCROLL_GROUP,
    /** Two tabs at the top of the attribute scroll: "Attributes" and "Effects". */
    SCROLL_TAB,
    /** No effect display. */
    OFF;

    @Override
    public Component getTranslatedName() {
        return Component.translatable("gearscope.configuration.effectDisplay." + this.name().toLowerCase(Locale.ROOT));
    }
}
