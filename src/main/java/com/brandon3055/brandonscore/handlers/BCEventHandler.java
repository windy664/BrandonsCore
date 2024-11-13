package com.brandon3055.brandonscore.handlers;

import com.brandon3055.brandonscore.inventory.ContainerBCTile;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.*;

/**
 * Created by brandon3055 on 26/08/2016.
 */
public class BCEventHandler {
    private static final CrashLock LOCK = new CrashLock("Already Initialized");

    public static Set<UUID> noClipPlayers = new HashSet<>();

    public static void init() {
        LOCK.lock();

        NeoForge.EVENT_BUS.addListener(BCEventHandler::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(BCEventHandler::livingUpdate);
        NeoForge.EVENT_BUS.addListener(BCEventHandler::onPlayerOpenContainer);

    }

    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        noClipPlayers.remove(event.getEntity().getUUID());
    }

    public static void livingUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player && noClipPlayers.contains(event.getEntity().getUUID())) {
            event.getEntity().noPhysics = true;
            ((Player) event.getEntity()).getAbilities().flying = true;
        }
    }

    public static void onPlayerOpenContainer(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof ContainerBCTile<?> container) {
            container.onOpened(event.getEntity());
        }
    }
}
