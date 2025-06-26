package com.brandon3055.brandonscore.inventory;

import com.brandon3055.brandonscore.api.BCStreamCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.covers1624.quack.collection.FastStream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Created by brandon3055 on 26/08/2016.
 * This is a dynamic inventory that will automatically expand its size to told any number of item stacks.
 */
public class InventoryDynamic implements Container {

    private List<ItemStack> stacks = new LinkedList<ItemStack>();
    public int xp = 0;

    public static final Codec<InventoryDynamic> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.list(ItemStack.CODEC).fieldOf("stacks").forGetter(e -> e.stacks),
            Codec.INT.fieldOf("xp").forGetter(e -> e.xp)
    ).apply(builder, InventoryDynamic::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryDynamic> STREAM_CODEC = BCStreamCodec.composite(
            ItemStack.LIST_STREAM_CODEC, e -> e.stacks,
            ByteBufCodecs.INT, e -> e.xp,
            InventoryDynamic::new
    );

    public InventoryDynamic() {
    }

    public InventoryDynamic(List<ItemStack> stacks, int xp) {
        this.stacks = stacks;
        this.xp = xp;
    }

    public InventoryDynamic copy() {
        return new InventoryDynamic(FastStream.of(stacks).map(ItemStack::copy).toList(), xp);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0) {
            return;
        }

        if (stack.isEmpty()) {
            if (index < stacks.size()) {
                stacks.remove(index);
            }
        } else if (index < stacks.size()) {
            stacks.set(index, stack);
        } else {
            stacks.add(stack);
        }
    }

    @Override
    public int getContainerSize() {
        return stacks.size() + 1;
    }

    @Override
    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack itemstack = getItem(index);

        if (!itemstack.isEmpty()) {
            if (itemstack.getCount() <= count) {
                setItem(index, ItemStack.EMPTY);
            } else {
                itemstack = itemstack.split(count);
                if (itemstack.getCount() == 0) {
                    setItem(index, ItemStack.EMPTY);
                }
            }
        }
        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = getItem(index);

        if (!stack.isEmpty()) {
            setItem(index, ItemStack.EMPTY);
        }

        return stack;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setChanged() {
        stacks.removeIf(ItemStack::isEmpty);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void startOpen(Player player) {

    }

    @Override
    public void stopOpen(Player player) {

    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return true;
    }


    @Override
    public void clearContent() {
        stacks.clear();
    }


    public void writeToNBT(HolderLookup.Provider provider, CompoundTag compound) {
        ListTag list = new ListTag();

        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && stack.getCount() > 0) {
                CompoundTag tag = new CompoundTag();
                list.add(stack.save(provider, tag));
            }
        }

        compound.put("InvItems", list);
    }

    public void readFromNBT(HolderLookup.Provider provider, CompoundTag compound) {
        ListTag list = compound.getList("InvItems", 10);
        stacks.clear();

        for (int i = 0; i < list.size(); i++) {
            stacks.add(ItemStack.parseOptional(provider, list.getCompound(i)));
        }
    }

    public void removeIf(Predicate<ItemStack> filter) {
        stacks.removeIf(filter);
    }

    public List<ItemStack> getStacks() {
        return stacks;
    }

    public void setStacks(LinkedList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InventoryDynamic that)) return false;
        return xp == that.xp && Objects.equals(stacks, that.stacks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stacks, xp);
    }
}
