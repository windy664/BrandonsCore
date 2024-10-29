package com.brandon3055.brandonscore.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by brandon3055 on 17/4/20.
 */
public class MultiCapabilityProvider implements INBTSerializable<CompoundTag> {

    private Map<ItemCapability<?, Void>, Object> capabilityMap = new HashMap<>();
    private Map<String, INBTSerializable<CompoundTag>> nameMap = new HashMap<>();

    public MultiCapabilityProvider() {}

    public MultiCapabilityProvider(INBTSerializable<CompoundTag> capInstance, String name, ItemCapability<?, Void>... capabilities) {
        addCapability(capInstance, name, capabilities);
    }

    public void addCapability(INBTSerializable<CompoundTag> capInstance, String name, ItemCapability<?, Void>... capabilities) {
        this.nameMap.put(name, capInstance);
        for (ItemCapability<?, Void> cap : capabilities) {
            Objects.requireNonNull(cap);
            this.capabilityMap.put(cap, capInstance);
        }
    }

    public <T> void addUnsavedCap(ItemCapability<T, Void> capability, T capInstance) {
        this.capabilityMap.put(capability, capInstance);
    }

    @Nullable
    public <T> T getCapability(ItemCapability<T, Void> cap, @Nullable Direction side) {
        if (capabilityMap.containsKey(cap)) {
            return (T)capabilityMap.get(cap);
        }
        return null;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        nameMap.forEach((s, t) -> tag.put(s, t.serializeNBT()));
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        nameMap.forEach((s, t) -> t.deserializeNBT(nbt.getCompound(s)));
    }
}
