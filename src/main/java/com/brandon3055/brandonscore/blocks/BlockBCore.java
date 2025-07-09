package com.brandon3055.brandonscore.blocks;

import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.api.IDataRetainingTile;
import com.brandon3055.brandonscore.lib.IBCoreBlock;
import com.brandon3055.brandonscore.lib.IChangeListener;
import com.brandon3055.brandonscore.lib.IInteractTile;
import com.brandon3055.brandonscore.lib.IRedstoneEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by brandon3055 on 18/3/2016.
 * This is the base block class form all blocks.
 */
public class BlockBCore extends Block implements IBCoreBlock {
    public static final DataComponentType<CustomData> BC_TILE_DATA_TAG = DataComponents.CUSTOM_DATA;
//    public static final String BC_TILE_DATA_TAG = "bc_tile_data";
    public static final String BC_MANAGED_DATA_FLAG = "bc_managed_data"; //Seemed like as good a place as any to put this.

    protected boolean canProvidePower = false;
    protected boolean isMobResistant = false;
    protected boolean isExplosionResistant = false;
    private boolean blockSpawns = false;
    private boolean isLightTransparent = false;

    public BlockBCore(Block.Properties properties) {
        super(properties);
    }

    public BlockBCore setLightTransparent() {
        isLightTransparent = true;
        return this;
    }

    public BlockBCore setMobResistant() {
        isMobResistant = true;
        return this;
    }

    public BlockBCore setExplosionResistant() {
        isExplosionResistant = true;
        return this;
    }

    @Override
    public float getShadeBrightness(BlockState p_60472_, BlockGetter p_60473_, BlockPos p_60474_) {
        return isLightTransparent ? 1F : super.getShadeBrightness(p_60472_, p_60473_, p_60474_);
    }

    /**
     * Prevents mobs from spawning on this block
     * */
    public BlockBCore dontSpawnOnMe() {
        this.blockSpawns = true;
        return this;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        BlockEntity tile = level.getBlockEntity(pos);

        if (tile instanceof IDataRetainingTile && !BrandonsCore.proxy.isCTRLKeyDown()) {
            CompoundTag tileData = new CompoundTag();
            ((IDataRetainingTile) tile).writeToItemStack(level.registryAccess(), tileData, false);
            if (!tileData.isEmpty()) {
                stack.set(BC_TILE_DATA_TAG, CustomData.of(tileData));
            }
        }

        if (tile instanceof Nameable && ((Nameable) tile).hasCustomName()) {
            stack.set(DataComponents.CUSTOM_NAME, ((Nameable) tile).getName());
        }

        return stack;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
        if (this instanceof EntityBlockBCore) {
            BlockEntity tile = world.getBlockEntity(pos);
            return tile instanceof IRedstoneEmitter;
        }

        return canProvidePower || super.canConnectRedstone(state, world, pos, side);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return canProvidePower;
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, SignalGetter world, BlockPos pos, Direction side) {
        if (this instanceof EntityBlockBCore) {
            BlockEntity tile = world.getBlockEntity(pos);
            return tile instanceof IChangeListener;
        }

        return super.shouldCheckWeakPower(state, world, pos, side);
    }

    @Override
    public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        if (this instanceof EntityBlockBCore) {
            BlockEntity tile = blockAccess.getBlockEntity(pos);
            if (tile instanceof IRedstoneEmitter) {
                return ((IRedstoneEmitter) tile).getWeakPower(blockState, side);
            }
        }
        return super.getSignal(blockState, blockAccess, pos, side);
    }

    @Override
    public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        if (this instanceof EntityBlockBCore) {
            BlockEntity tile = blockAccess.getBlockEntity(pos);
            if (tile instanceof IRedstoneEmitter) {
                return ((IRedstoneEmitter) tile).getStrongPower(blockState, side);
            }
        }
        return super.getDirectSignal(blockState, blockAccess, pos, side);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (this instanceof EntityBlockBCore) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof IChangeListener) {
                ((IChangeListener) tile).onNeighborChange(fromPos);
            }
        }
        super.neighborChanged(state, world, pos, blockIn, fromPos, isMoving);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (this instanceof EntityBlockBCore) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof IInteractTile) {
                return ((IInteractTile) tile).useItemOn(stack, state, player, hand, hit);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (this instanceof EntityBlockBCore) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof IInteractTile) {
                return ((IInteractTile) tile).useWithoutItem(state, player, hit);
            }
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public void attack(BlockState state, Level world, BlockPos pos, Player player) {
        if (this instanceof EntityBlockBCore) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof IInteractTile) {
                ((IInteractTile) tile).onBlockAttack(state, player);
            }
        }
        super.attack(state, world, pos, player);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        BlockEntity tile = world.getBlockEntity(pos);

        if (tile instanceof IDataRetainingTile) {
            if (stack.has(BC_TILE_DATA_TAG) && !stack.get(BC_TILE_DATA_TAG).isEmpty()) {
                ((IDataRetainingTile) tile).readFromItemStack(world.registryAccess(), stack.get(BC_TILE_DATA_TAG).copyTag());
            }
        }

        if (tile instanceof TileBCore && stack.has(DataComponents.CUSTOM_NAME)) {
            ((TileBCore) tile).setCustomName(stack.getHoverName().getString());
        }
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, BlockEntity te, ItemStack heldStack) {
        ItemStack stack = null;

        if (te instanceof IDataRetainingTile && ((IDataRetainingTile) te).saveToItem()) {
            CompoundTag tileData = new CompoundTag();
            ((IDataRetainingTile) te).writeToItemStack(world.registryAccess(), tileData, true);
            if (!tileData.isEmpty()) {
                stack = new ItemStack(this, 1);//, damageDropped(state));
                stack.set(BC_TILE_DATA_TAG, CustomData.of(tileData));
            }
        }

        if (te instanceof Nameable && ((Nameable) te).hasCustomName()) {
            if (stack == null) {
                stack = new ItemStack(this, 1);
            }
            stack.set(DataComponents.CUSTOM_NAME, ((Nameable) te).getName());
        }

        if (stack != null) {
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);

            popResource(world, pos, stack);
            //Remove tile to make sure no one else can mess with it and dupe its contents.
            world.removeBlockEntity(pos);
        } else {
            super.playerDestroy(world, player, pos, state, te, heldStack);
        }
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter world, BlockPos pos, Entity entity) {
        if (!isMobResistant) {
            return super.canEntityDestroy(state, world, pos, entity);
        }
        return entity instanceof Player;
    }

    @Override
    public void onBlockExploded(BlockState state, Level world, BlockPos pos, Explosion explosion) {
        if ((explosion.getIndirectSourceEntity() instanceof Mob && isMobResistant) || isExplosionResistant) {
            return;
        }
        super.onBlockExploded(state, world, pos, explosion);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        if ((explosion.getIndirectSourceEntity() instanceof Mob && isMobResistant) || isExplosionResistant) {
            return false;
        }
        return super.dropFromExplosion(explosion);
    }

    public static int getRedstonePower(LevelReader world, BlockPos pos, Direction facing) {
        BlockState blockstate = world.getBlockState(pos);
        return blockstate.shouldCheckWeakPower(world, pos, facing) ? getStrongPower(world, pos) : blockstate.getSignal(world, pos, facing);
    }

    public static int getStrongPower(LevelReader world, BlockPos pos) {
        int i = 0;
        i = Math.max(i, world.getDirectSignal(pos.below(), Direction.DOWN));
        if (i >= 15) {
            return i;
        } else {
            i = Math.max(i, world.getDirectSignal(pos.above(), Direction.UP));
            if (i >= 15) {
                return i;
            } else {
                i = Math.max(i, world.getDirectSignal(pos.north(), Direction.NORTH));
                if (i >= 15) {
                    return i;
                } else {
                    i = Math.max(i, world.getDirectSignal(pos.south(), Direction.SOUTH));
                    if (i >= 15) {
                        return i;
                    } else {
                        i = Math.max(i, world.getDirectSignal(pos.west(), Direction.WEST));
                        if (i >= 15) {
                            return i;
                        } else {
                            return Math.max(i, world.getDirectSignal(pos.east(), Direction.EAST));
                        }
                    }
                }
            }
        }
    }

    public static boolean isBlockPowered(LevelReader world, BlockPos pos) {
        if (getRedstonePower(world, pos.below(), Direction.DOWN) > 0) {
            return true;
        } else if (getRedstonePower(world, pos.above(), Direction.UP) > 0) {
            return true;
        } else if (getRedstonePower(world, pos.north(), Direction.NORTH) > 0) {
            return true;
        } else if (getRedstonePower(world, pos.south(), Direction.SOUTH) > 0) {
            return true;
        } else if (getRedstonePower(world, pos.west(), Direction.WEST) > 0) {
            return true;
        } else {
            return getRedstonePower(world, pos.east(), Direction.EAST) > 0;
        }
    }

    @OnlyIn (Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        if (stack.has(BC_TILE_DATA_TAG)) {
            tooltip.add(Component.translatable("info.brandonscore.block_has_saved_data"));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public boolean renderSelectionBox(RenderHighlightEvent.Block event, Level level) {
        return true;
    }
}
