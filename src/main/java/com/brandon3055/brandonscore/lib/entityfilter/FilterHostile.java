package com.brandon3055.brandonscore.lib.entityfilter;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Created by brandon3055 on 7/11/19.
 */
public class FilterHostile extends FilterBase {

    protected boolean whitelistHostile = true;

    public FilterHostile(EntityFilter filter) {
        super(filter);
    }

    public void setWhitelistHostile(boolean whitelistHostile) {
        boolean prev = this.whitelistHostile;
        this.whitelistHostile = whitelistHostile;
        getFilter().nodeModified(this);
        this.whitelistHostile = prev;
    }

    public boolean isWhitelistHostile() {
        return whitelistHostile;
    }

    @Override
    public boolean test(Entity entity) {
        if (entity instanceof Player) return false;
        boolean isHostile = entity instanceof Enemy;
        return isHostile == whitelistHostile;
    }

    @Override
    public FilterType getType() {
        return FilterType.HOSTILE;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag compound = super.serializeNBT(provider);
        compound.putBoolean("include", whitelistHostile);
        return compound;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        super.deserializeNBT(provider, nbt);
        whitelistHostile = nbt.getBoolean("include");
    }

    @Override
    public void serializeMCD(MCDataOutput output) {
        super.serializeMCD(output);
        output.writeBoolean(whitelistHostile);
    }

    @Override
    public void deSerializeMCD(MCDataInput input) {
        super.deSerializeMCD(input);
        whitelistHostile = input.readBoolean();
    }
}
