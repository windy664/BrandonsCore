package com.brandon3055.brandonscore.lib;

import com.brandon3055.brandonscore.blocks.BlockBCore;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Implement this on a tile that uses {@link BlockBCore} and you will be able to receive block use and block attack calls.
 * Created by brandon3055 on 21/10/2016.
 */
public interface IInteractTile {

    /**
     * Passed through from Block#useItemOn, Despite what the name may suggest, this method gets called regardless of weather the player is holding an item or not.
     * The stack will simply be empty if the player is not holding an item.
     */
    default ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Passed through from Block#useWithoutItem, also, just like useItemOn, this gets called regardless of weather the player is holding an item.
     * But this does only get called if the interaction was not consumed by useItemOn, which gets called first.
     */
    default InteractionResult useWithoutItem(BlockState state, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    /**
     * Called when the host block is attacked (left clicked)
     */
    default void onBlockAttack(BlockState state, Player player) {}

}
