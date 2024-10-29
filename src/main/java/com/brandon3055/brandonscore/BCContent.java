package com.brandon3055.brandonscore;

import com.brandon3055.brandonscore.inventory.ContainerPlayerAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.brandon3055.brandonscore.BrandonsCore.MODID;

/**
 * Created by brandon3055 on 23/12/19.
 */
public class BCContent {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerPlayerAccess>> MENU_PLAYER_ACCESS = MENU_TYPES.register("player_access", () -> IMenuTypeExtension.create(ContainerPlayerAccess::new));

    public static void init(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }

}
