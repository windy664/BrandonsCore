package com.brandon3055.brandonscore.capability;

import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.api.power.IOPStorage;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Created by brandon3055 on 14/8/19.
 * <p>
 * Operational Potential is the power system used by Draconic Evolution and related mods.
 * This system is an extension of Forge Energy that allows long based power transfer and storage.
 */
public class CapabilityOP {

    public static final BlockCapability<IOPStorage, @Nullable Direction> BLOCK = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(BrandonsCore.MODID, "op"), IOPStorage.class);
    public static final EntityCapability<IOPStorage, @Nullable Direction> ENTITY = EntityCapability.createSided(ResourceLocation.fromNamespaceAndPath(BrandonsCore.MODID, "op"), IOPStorage.class);
    public static final ItemCapability<IOPStorage, Void> ITEM = ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath(BrandonsCore.MODID, "op"), IOPStorage.class);

    private CapabilityOP() {}

    @Nullable
    public static IOPStorage fromBlockEntity(BlockEntity blockEntity, @Nullable Direction direction) {
        return BLOCK.getCapability(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
    }

    @Nullable
    public static IOPStorage fromBlockEntity(BlockEntity blockEntity) {
        return fromBlockEntity(blockEntity, null);
    }
}
