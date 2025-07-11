package com.brandon3055.brandonscore.api;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;

import java.util.Locale;
import java.util.function.IntFunction;

import static net.minecraft.ChatFormatting.*;
import static net.minecraft.world.item.Rarity.*;

/**
 * Created by brandon3055 on 8/02/19.
 * <p>
 * These are the definitions for the different tech levels in Draconic Evolution.
 * Also to make this a little less confusing i will be switching up the core names a little in 1.14+
 * They are now
 * Tier 0: Draconium Core
 * Tier 1: Wyvern Core
 * Tier 2: Draconic Core
 * Tier 3: Chaotic Core
 */
public enum TechLevel implements StringRepresentable {
    //@formatter:off
    /**
     * Basic / Draconium level.
     */
    DRACONIUM   (0, "draconium",    WHITE,       COMMON,   3),
    /**
     * Wyvern can be thought of as "Nether Star tier"
     * Though that does not necessarily mean all wyvern tier items
     * require nether stars. Take wyvern energy crystals for example.
     */
    WYVERN      (1, "wyvern",       BLUE,        UNCOMMON, 32),
    /**
     * AKA Awakened. Pretty self explanatory. Draconic is the tier above wyvern and in most cases
     * draconic tier items should require awakened draconium to craft.
     */
    DRACONIC    (2, "draconic",     GOLD,        RARE,     128),
    /**
     * Chaotic is the ultimate end game tier.
     * Obviously all chaotic tier items require chaos shards or fragments to craft.
     */
    CHAOTIC     (3, "chaotic",      DARK_PURPLE, EPIC,     512);
    //@formatter:on

    public static final Codec<TechLevel> CODEC = StringRepresentable.fromValues(TechLevel::values);
    public static final IntFunction<TechLevel> BY_ID = ByIdMap.continuous(e -> e.index, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, TechLevel> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, e -> e.index);

    public final int index;
    private final String name;
    private final ChatFormatting textColour;
    private Rarity rarity;
    private final int harvestLevel;
    private Tier itemTier;
    public static final TechLevel[] VALUES = new TechLevel[4];
    public static final TechLevel[] TOOL_LEVELS = new TechLevel[3];

    static {
        for (TechLevel tier : values()) {
            VALUES[tier.index] = tier;
            if (tier != DRACONIUM) {
                TOOL_LEVELS[tier.index - 1] = tier;
            }
        }
    }

    TechLevel(int index, String name, ChatFormatting colour, Rarity rarity, int harvestLevel) {
        this.index = index;
        this.name = name;
        this.textColour = colour;
        this.rarity = rarity;
        this.harvestLevel = harvestLevel;
    }

    public ChatFormatting getTextColour() {
        return textColour;
    }

    public Component getDisplayName() {
        return Component.translatable("tech_level.draconicevolution." + name().toLowerCase(Locale.ENGLISH));
    }

    public Rarity getRarity() {
        return rarity;
    }

    //The mining level does not really mean much as most mods dont do anything special with it.
    public int getHarvestLevel() {
        return harvestLevel;
    }

    public static TechLevel byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : DRACONIUM;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
