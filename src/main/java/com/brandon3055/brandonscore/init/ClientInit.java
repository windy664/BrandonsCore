package com.brandon3055.brandonscore.init;

import com.brandon3055.brandonscore.client.*;
import com.brandon3055.brandonscore.client.hud.HudManager;
import com.brandon3055.brandonscore.client.model.EquippedItemModelLayer;
import com.brandon3055.brandonscore.client.shader.BCShaders;
import com.brandon3055.brandonscore.handlers.contributor.ContributorHandler;
import com.brandon3055.brandonscore.lib.DLRSCache;
import com.brandon3055.brandonscore.utils.BCProfiler;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Created by brandon3055 on 15/11/2022
 */
public class ClientInit {
    private static final CrashLock LOCK = new CrashLock("Already Initialized.");

    public static void init(IEventBus modBus) {
        LOCK.lock();

        modBus.addListener(BCGuiTextures::onResourceReload);
        modBus.addListener(ClientInit::clientSetupEvent);
        modBus.addListener(ClientInit::onAddRenderLayers);

        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> ContributorHandler.onClientLogin(event.getPlayer()));
        ProcessHandlerClient.init();
        HudManager.init(modBus);
        BCShaders.init(modBus);
        BCProfiler.init();
        DLRSCache.init();
    }

    private static void clientSetupEvent(FMLClientSetupEvent event) {
        BCClientEventHandler.init();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void onAddRenderLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            LivingEntityRenderer renderer = event.getSkin(skin);
            assert renderer != null;
            renderer.addLayer(new EquippedItemModelLayer(renderer, skin.id().equals("slim"))); //TODO Does this work?
        }

        for (EntityRenderer r : Minecraft.getInstance().getEntityRenderDispatcher().renderers.values()) {
            if (r instanceof LivingEntityRenderer<?, ?> renderer && renderer.getModel() instanceof HumanoidModel) {
                renderer.addLayer(new EquippedItemModelLayer(renderer, false));
            }
        }
    }
}
