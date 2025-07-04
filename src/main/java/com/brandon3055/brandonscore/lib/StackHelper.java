package com.brandon3055.brandonscore.lib;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DynamicOps;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

/**
 * Introducing, The new and improved replacement for StringyStacks!<br>
 * Now supporting DataComponents!<br><br>
 * Valid Formats:                                               <br>
 * domain:path                                                  <br>
 * domain:path,[count]                                          <br>
 * domain:path[Data]                                            <br>
 * domain:path[Data],[count]                                    <br>
 * <p>
 * Examples:                                                                                                                <br>
 * minecraft:diamond_sword                                                                                                  <br>
 * minecraft:diamond_sword[minecraft:enchantments={levels:{"minecraft:sharpness":5}}]                                       <br>
 * minecraft:stick,64                                                                                                       <br>
 * minecraft:light_blue_banner[minecraft:banner_patterns=[{color:"white",pattern:"minecraft:stripe_center"}]]               <br>
 * minecraft:light_blue_banner[minecraft:banner_patterns=[{color:"white",pattern:"minecraft:stripe_center"}]],8             <br>
 * <p>
 * This is the exact same format as used by the /give command, except when
 * specifying a count, you use a comma instead or a space as the separator before the count.
 * <p>
 * Created by brandon3055 on 27/06/2025
 */
public class StackHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ItemParser parser;
    private final DynamicOps<Tag> registryOps;
    private boolean logErrors;

    /**
     * If you plan on using this a lot within a specific context,
     * then you may want to consider stack helper instance for efficiency.
     * Just be aware, you can only hold onto it so long as the given registry access remains valid.
     * I'm not sure if/when registry access is invalidated, but if I had to guess, world unload.
     * <p>
     * For one-offs the static methods are fine to use.
     *
     * @param lookupProvider Registry Access
     */
    public StackHelper(HolderLookup.Provider lookupProvider) {
        this(lookupProvider, true);
    }

    /**
     * If you plan on using this a lot within a specific context,
     * then you may want to consider stack helper instance for efficiency.
     * Just be aware, you can only hold onto it so long as the given registry access remains valid.
     * I'm not sure if/when registry access is invalidated, but if I had to guess, world unload.
     * <p>
     * For one-offs the static methods are fine to use.
     *
     * @param lookupProvider Registry Access
     * @param logErrors      Log parsing errors.
     */
    public StackHelper(HolderLookup.Provider lookupProvider, boolean logErrors) {
        this.parser = new ItemParser(lookupProvider);
        this.registryOps = lookupProvider.createSerializationContext(NbtOps.INSTANCE);
        this.logErrors = logErrors;
    }

    public ItemStack parse(String stackString) {
        return parse(stackString, parser, ItemStack.EMPTY, logErrors);
    }

    public ItemStack parse(String stackString, ItemStack fallback) {
        return parse(stackString, parser, fallback, logErrors);
    }

    public String encode(ItemStack stack) {
        return encode(stack, registryOps);
    }

    //=== From String Static Methods ===//

    public static ItemStack parse(String stackString, RegistryAccess registryAccess) {
        return parse(stackString, registryAccess, ItemStack.EMPTY);
    }

    public static ItemStack parse(String stackString, RegistryAccess registryAccess, ItemStack fallback) {
        return parse(stackString, new ItemParser(registryAccess), fallback, true);
    }

    public static ItemStack parse(String stackString, ItemParser parser) {
        return parse(stackString, parser, ItemStack.EMPTY, true);
    }

    public static ItemStack parse(String stackString, ItemParser parser, ItemStack fallback, boolean logErrors) {
        int count = 1;
        int comma = stackString.lastIndexOf(',');
        if (comma != -1) {
            String number = stackString.substring(comma + 1);
            if (number.matches("-?\\d+(\\d+)?")) {
                count = Integer.parseInt(number);
                stackString = stackString.substring(0, comma);
            }
        }

        try {
            ItemParser.ItemResult result = parser.parse(new StringReader(stackString));
            ItemStack stack = new ItemStack(result.item(), count);
            if (count > stack.getMaxStackSize()) {
                stack.setCount(stack.getMaxStackSize());
            }
            stack.applyComponents(result.components());
            return stack;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Failed to parse stack string: {}", stackString, e);
            return fallback;
        }
    }

    //=== To String Static Methods ===//

    public static String encode(ItemStack stack, RegistryAccess registryAccess) {
        return encode(stack, registryAccess.createSerializationContext(NbtOps.INSTANCE));
    }

    public static String encode(ItemStack stack, DynamicOps<Tag> registryOps) {
        String stackString = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        DataComponentPatch componentPatch = stack.getComponentsPatch();
        if (!componentPatch.isEmpty()) {
            Tag tag = DataComponentPatch.CODEC.encodeStart(registryOps, componentPatch).result().orElseGet(CompoundTag::new);
            if (tag instanceof CompoundTag compound && !compound.isEmpty()) {
                stackString = stackString + "[" + compound.getAllKeys().stream().map(key -> key + "=" + compound.get(key)).collect(Collectors.joining(",")) + "]";
            }
        }

        if (stack.getCount() != 1) {
            stackString += "," + stack.getCount();
        }

        return stackString;
    }
}