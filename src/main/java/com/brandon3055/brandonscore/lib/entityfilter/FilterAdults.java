package com.brandon3055.brandonscore.lib.entityfilter;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Created by brandon3055 on 7/11/19.
 */
public class FilterAdults extends FilterBase {

    protected boolean whitelistAdults = true;
    protected boolean includeNonAgeable = true;

    public FilterAdults(EntityFilter filter) {
        super(filter);
    }

    public void setWhitelistAdults(boolean whitelistAdults) {
        boolean prev = this.whitelistAdults;
        this.whitelistAdults = whitelistAdults;
        getFilter().nodeModified(this);
        this.whitelistAdults = prev;
    }

    public void setIncludeNonAgeable(boolean includeNonAgeable) {
        boolean prev = this.includeNonAgeable;
        this.includeNonAgeable = includeNonAgeable;
        getFilter().nodeModified(this);
        this.includeNonAgeable = prev;
    }

    public boolean isWhitelistAdults() {
        return whitelistAdults;
    }

    public boolean isIncludeNonAgeable() {
        return includeNonAgeable;
    }

    @Override
    public boolean test(Entity entity) {
        if (entity instanceof Player) return false;
        boolean isAgeable = entity instanceof AgeableMob;
        if (isAgeable) {
            AgeableMob ageable = (AgeableMob) entity;
            return whitelistAdults == !ageable.isBaby();
        }
        return includeNonAgeable;
    }

    @Override
    public FilterType getType() {
        return FilterType.ADULTS;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag compound = super.serializeNBT(provider);
        compound.putBoolean("include", whitelistAdults);
        compound.putBoolean("tamable", includeNonAgeable);
        return compound;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        super.deserializeNBT(provider, nbt);
        whitelistAdults = nbt.getBoolean("include");
        includeNonAgeable = nbt.getBoolean("tamable");
    }

    @Override
    public void serializeMCD(MCDataOutput output) {
        super.serializeMCD(output);
        output.writeBoolean(whitelistAdults);
        output.writeBoolean(includeNonAgeable);
    }

    @Override
    public void deSerializeMCD(MCDataInput input) {
        super.deSerializeMCD(input);
        whitelistAdults = input.readBoolean();
        includeNonAgeable = input.readBoolean();
    }
}
