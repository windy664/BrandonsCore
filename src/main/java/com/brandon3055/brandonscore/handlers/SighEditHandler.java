package com.brandon3055.brandonscore.handlers;

import com.brandon3055.brandonscore.BrandonsCore;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Created by brandon3055 on 24/01/2023
 */
public class SighEditHandler {

    private static final CrashLock LOCK = new CrashLock("Already Initialized");
    private static final GameRules.Key<GameRules.BooleanValue> ALLOW_SIGN_EDIT = GameRules.register(BrandonsCore.MODID + ":allowSignEditing", GameRules.Category.MISC, GameRules.BooleanValue.create(false, (server, newValue) -> {}));

    public static void init() {
        LOCK.lock();
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, SighEditHandler::onBlockInteract);
    }

    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || event.isCanceled()) {
            return;
        }

        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !player.getAbilities().mayBuild || !level.getGameRules().getBoolean(ALLOW_SIGN_EDIT) || !player.getItemInHand(event.getHand()).isEmpty()) {
            return;
        }

        BlockEntity entity = level.getBlockEntity(event.getHitVec().getBlockPos());
        if (entity instanceof SignBlockEntity signEntity) {
//            signEntity.setEditable(true);
            player.openTextEdit(signEntity, true); //TODO Side
            event.setCanceled(true);
        }
    }
}
