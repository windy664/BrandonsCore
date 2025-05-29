package com.brandon3055.brandonscore.utils;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.api.math.Vector2;
import com.brandon3055.brandonscore.lib.TeleportUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

import javax.annotation.Nullable;
import java.util.Optional;

public record TargetPos(Vector3 pos, ResourceKey<Level> dimension, Optional<Vector2> facing) {

    public static final Codec<TargetPos> CODEC = RecordCodecBuilder.create(b -> b.group(
            Vector3.CODEC.fieldOf("pos").forGetter(TargetPos::pos),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(TargetPos::dimension),
            Vector2.CODEC.optionalFieldOf("facing").forGetter(TargetPos::facing)
            ).apply(b, TargetPos::new)
    );

    public static final StreamCodec<ByteBuf, TargetPos> STREAM_CODEC = StreamCodec.composite(
            Vector3.STREAM_CODEC, TargetPos::pos,
            ResourceKey.streamCodec(Registries.DIMENSION), TargetPos::dimension,
            ByteBufCodecs.optional(Vector2.STREAM_CODEC), TargetPos::facing,
            TargetPos::new
    );

    public static TargetPos of(Entity entity) {
        return of(entity, true);
    }

    public static TargetPos of(Entity entity, boolean includeHeading) {
        return new TargetPos(Vector3.fromEntity(entity), entity.level().dimension(), includeHeading ? Optional.of(new Vector2(entity.getXRot(), entity.getYRot())) : Optional.empty());
    }

    public static TargetPos of(double x, double y, double z, ResourceKey<Level> dimension) {
        return new TargetPos(new Vector3(x, y, z), dimension, Optional.empty());
    }

    public static TargetPos of(double x, double y, double z, ResourceKey<Level> dimension, Vec2 facing) {
        return new TargetPos(new Vector3(x, y, z), dimension, Optional.of(new Vector2(facing)));
    }

    public static TargetPos of(Vector3 pos, ResourceKey<Level> dimension) {
        return new TargetPos(pos, dimension, Optional.empty());
    }

    public static TargetPos of(Vector3 pos, ResourceKey<Level> dimension, Vec2 facing) {
        return new TargetPos(pos, dimension, Optional.of(new Vector2(facing)));
    }

    public static TargetPos of(CompoundTag nbt) {
        return readFromNBT(nbt);
    }


    public double getX() {
        return pos.x;
    }

    public double getY() {
        return pos.y;
    }

    public double getZ() {
        return pos.z;
    }

    public Vector3 getPos() {
        return pos;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

//    public float getPitch() {
//        return pitch;
//    }
//
//    public float getYaw() {
//        return yaw;
//    }

    public String getReadableName(boolean fullDim) {
        return "X: " + (int) Math.floor(pos.x) +
                ", Y: " + (int) Math.floor(pos.y) +
                ", Z: " + (int) Math.floor(pos.z) +
                ", " + (fullDim ? dimension.location() : dimension.location().getPath());
    }

//    public TargetPos setIncludeHeading(boolean includeHeading) {
//        this.includeHeading = includeHeading;
//        return this;
//    }
//
//    public TargetPos setX(double x) {
//        pos.x = x;
//        return this;
//    }
//
//    public TargetPos setY(double y) {
//        pos.y = y;
//        return this;
//    }
//
//    public TargetPos setZ(double z) {
//        pos.z = z;
//        return this;
//    }
//
//    public TargetPos setPos(Vector3 pos) {
//        this.pos = pos;
//        return this;
//    }
//
//    public TargetPos setDimension(ResourceKey<Level> d) {
//        dimension = d;
//        return this;
//    }
//
//    public TargetPos setPitch(float p) {
//        pitch = p;
//        return this;
//    }
//
//    public TargetPos setYaw(float y) {
//        yaw = y;
//        return this;
//    }

    public CompoundTag writeToNBT(CompoundTag nbt) {
        pos.writeToNBT(nbt);
        nbt.putString("dim", dimension.location().toString());
        facing.ifPresent(e -> {
            nbt.putDouble("facing_x", e.x);
            nbt.putDouble("facing_y", e.y);
        });
        return nbt;
    }

    public CompoundTag writeToNBT() {
        return writeToNBT(new CompoundTag());
    }

    public static TargetPos readFromNBT(CompoundTag nbt) {
        Vector3 pos = Vector3.fromNBT(nbt);
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(nbt.getString("dim")));
        if (nbt.contains("facing_x")) {
            return new TargetPos(pos, dimension, Optional.of(new Vector2(nbt.getDouble("facing_x"), nbt.getDouble("facing_y"))));
        }
        return new TargetPos(pos, dimension, Optional.empty());
    }

    public void write(MCDataOutput output) {
        output.writeVector(pos);
        output.writeResourceLocation(dimension.location());
        output.writeBoolean(facing.isPresent());
        facing.ifPresent(e -> {
            output.writeDouble(e.x);
            output.writeDouble(e.y);
        });
    }

    public static TargetPos read(MCDataInput input) {
        Vector3 pos = input.readVector();
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, input.readResourceLocation());
        if (input.readBoolean()) {
            return new TargetPos(pos, dimension, Optional.of(new Vector2(input.readDouble(), input.readDouble())));
        }
        return new TargetPos(pos, dimension, Optional.empty());
    }

    public Entity teleport(Entity entity) {
        if (facing.isPresent()) {
            return TeleportUtils.teleportEntity(entity, dimension, pos, (float) facing.get().y, (float) facing.get().x);
        }
        return TeleportUtils.teleportEntity(entity, dimension, pos);
    }
}
