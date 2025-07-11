package com.brandon3055.brandonscore.lib.datamanager;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by brandon3055 on 08/10/2019.
 * This is only used for synchronization purposes though it could theoretically be used to save values as well.
 */
@Deprecated //Avoid if possible
public class ManagedNBTSerializableMap extends AbstractManagedData {

    private Map<String, INBTSerializable<CompoundTag>> valueMap;
    private final RegistryAccess registryAccess;
    private Map<String, Tag> lastValueMap;

    public ManagedNBTSerializableMap(String name, Map<String, INBTSerializable<CompoundTag>> serializableMap, RegistryAccess registryAccess, DataFlags... flags) {
        super(name, flags);
        this.valueMap = serializableMap;
        this.registryAccess = registryAccess;
        lastValueMap = new HashMap<>();
        serializableMap.forEach((key, value) -> lastValueMap.put(key, value.serializeNBT(registryAccess)));
    }

    public Map<String, INBTSerializable<CompoundTag>> get() {
        return valueMap;
    }

    @Override
    public void validate() {}

    @Override
    public boolean isDirty(boolean reset) {
        if (lastValueMap != null && (lastValueMap.size() != valueMap.size() || (valueMap.entrySet().stream().anyMatch(entry -> {
            Tag base = lastValueMap.get(entry.getKey());
            return base == null || !(base.equals(entry.getValue().serializeNBT(registryAccess)));
        })))) {
            if (reset) {
                lastValueMap.clear();
                valueMap.forEach((key, value) -> lastValueMap.put(key, value.serializeNBT(registryAccess)));
            }
            return true;
        }

        return super.isDirty(reset);
    }

    @Override
    public void toBytes(MCDataOutput output) {
        output.writeVarInt(valueMap.size());
        valueMap.forEach((name, serializable) -> output.writeString(name).writeCompoundNBT(serializable.serializeNBT(registryAccess)));
    }

    @Override
    public void fromBytes(MCDataInput input) {
        int c = input.readVarInt();
        for (int i = 0; i < c; i++) {
            String name = input.readString();
            CompoundTag nbt = input.readCompoundNBT();
            if (valueMap.containsKey(name)) {
                valueMap.get(name).deserializeNBT(registryAccess, nbt);
            }
        }
        lastValueMap.clear();
        valueMap.forEach((key, value) -> lastValueMap.put(key, value.serializeNBT(registryAccess)));
    }

    @Override
    public void toNBT(HolderLookup.Provider provider, CompoundTag compound) {
        CompoundTag tags = new CompoundTag();
        valueMap.forEach((name, serializable) -> tags.put(name, serializable.serializeNBT(registryAccess)));
        compound.put(name, tags);
    }

    @Override
    public void fromNBT(HolderLookup.Provider provider, CompoundTag compound) {
        CompoundTag tags = compound.getCompound(name);
        for (String name : new ArrayList<>(valueMap.keySet())) {
            if (tags.contains(name)) {
                valueMap.get(name).deserializeNBT(registryAccess, tags.getCompound(name));
            }
        }
        lastValueMap.clear();
        valueMap.forEach((key, value) -> lastValueMap.put(key, value.serializeNBT(registryAccess)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":[" + getName() + "=" + valueMap + "]";
    }
}
