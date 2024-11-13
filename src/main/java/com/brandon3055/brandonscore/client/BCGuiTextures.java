package com.brandon3055.brandonscore.client;

import codechicken.lib.gui.modular.sprite.GuiTextures;
import codechicken.lib.gui.modular.sprite.Material;
import com.brandon3055.brandonscore.BCConfig;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

import static com.brandon3055.brandonscore.BrandonsCore.MODID;

/**
 * Created by brandon3055 on 3/09/2016.
 */
public class BCGuiTextures {

    public static final GuiTextures TEXTURES = new GuiTextures(MODID);

    public static void init(IEventBus modBus) {
        TEXTURES.init(modBus);
    }

    public static Material get(String texture) {
        return TEXTURES.get(texture);
    }

    public static Supplier<Material> getter(Supplier<String> texture) {
        return TEXTURES.getter(texture);
    }

    public static Supplier<Material> getter(String texture) {
        return () -> get(texture);
    }

    public static Material getThemed(String location) {
        return get((BCConfig.darkMode ? "dark/" : "light/") + location);
    }

    public static Supplier<Material> themedGetter(String location) {
        return () -> get((BCConfig.darkMode ? "dark/" : "light/") + location);
    }
}
