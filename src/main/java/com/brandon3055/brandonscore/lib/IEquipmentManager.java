package com.brandon3055.brandonscore.lib;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Created by brandon3055 on 18/1/21
 */
public interface IEquipmentManager {

//    void addEquipCaps(ItemStack stack, MultiCapabilityProvider provider);

    Optional<IItemHandlerModifiable> getInventory(LivingEntity entity);

    ItemStack findMatchingItem(Item item, LivingEntity entity);

    ItemStack findMatchingItem(Predicate<ItemStack> predicate, LivingEntity entity);

    List<ResourceLocation> getSlotIcons(LivingEntity entity);

    void registerCap(RegisterCapabilitiesEvent event, Item item);
}
