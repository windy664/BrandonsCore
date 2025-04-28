package com.brandon3055.brandonscore.lib;

import com.brandon3055.brandonscore.blocks.BlockBCore;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Implement this on a tile that uses {@link BlockBCore} and you will be able to receive block use and block attack calls.
 * Created by brandon3055 on 21/10/2016.
 */
public interface IInteractTile {

    /**
     * Generic use method that gets called when block is right-clicked with or without an item.
     */
    default InteractionResult useGeneric(BlockState state, Player player, BlockHitResult hit) {
        return InteractionResult.FAIL;
    }

    default ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Player player, InteractionHand hand, BlockHitResult hit) {
        return switch (useGeneric(state, player, hit)) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case FAIL -> ItemInteractionResult.FAIL;
        };
    }

    default InteractionResult useWithoutItem(BlockState state, Player player, BlockHitResult hit) {
        return useGeneric(state, player, hit);
    }

    /**
     * Called when the host block is attacked (left clicked)
     */
    default void onBlockAttack(BlockState state, Player player) {}

}
