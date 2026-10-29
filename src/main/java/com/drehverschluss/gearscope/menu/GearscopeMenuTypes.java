package com.drehverschluss.gearscope.menu;

import com.drehverschluss.gearscope.Gearscope;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the Gearscope menu type. Deferred registration is wired to the mod event bus from
 * {@link Gearscope}.
 */
public final class GearscopeMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, Gearscope.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<GearscopeMenu>> CHARACTER =
            MENU_TYPES.register("character", () -> IMenuTypeExtension.create(GearscopeMenu::fromNetwork));

    private GearscopeMenuTypes() {
    }
}
