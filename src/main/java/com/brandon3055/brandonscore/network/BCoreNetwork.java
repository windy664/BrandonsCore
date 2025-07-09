package com.brandon3055.brandonscore.network;

import codechicken.lib.internal.network.ClientConfigurationPacketHandler;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustomChannel;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.handlers.contributor.ContributorProperties;
import com.brandon3055.brandonscore.multiblock.MultiBlockDefinition;
import com.brandon3055.brandonscore.multiblock.MultiBlockManager;
import com.brandon3055.brandonscore.utils.LogHelperBC;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;

import java.util.Map;

/**
 * Created by brandon3055 on 17/12/19.
 */
public class BCoreNetwork {
    private static final CrashLock LOCK = new CrashLock("Already Initialized.");

    public static final ResourceLocation CHANNEL_NAME = ResourceLocation.fromNamespaceAndPath(BrandonsCore.MODID, "network");
    public static final PacketCustomChannel CHANNEL = new PacketCustomChannel(CHANNEL_NAME)
            .optional()
            .versioned(BrandonsCore.container().getModInfo().getVersion().toString())
            .clientConfiguration(() -> ClientConfigurationPacketHandler::new)
            .client(() -> ClientPacketHandler::new)
            .server(() -> ServerPacketHandler::new);

    //Server to client
    public static final int C_TILE_DATA_MANAGER = 1;
    public static final int C_TILE_MESSAGE = 2;
    public static final int C_NO_CLIP = 4;
    public static final int C_PLAYER_ACCESS = 5;
    public static final int C_PLAYER_ACCESS_UPDATE = 6;
    public static final int C_INDEXED_MESSAGE = 7;
    public static final int C_TILE_CAP_DATA = 8;
    public static final int C_PLAY_SOUND = 9;
    public static final int C_SPAWN_ENTITY = 10;
    public static final int C_SPAWN_PARTICLE = 11;
    public static final int C_ENTITY_VELOCITY = 12;
    public static final int C_OPEN_HUD_CONFIG = 13;
    public static final int C_MULTI_BLOCK_DEFINITIONS = 14;
    public static final int C_CONTRIBUTOR_CONFIG = 15;
    //Client to server
    public static final int S_CONTAINER_MESSAGE = 1;
    public static final int S_PLAYER_ACCESS_BUTTON = 2;
    public static final int S_TILE_DATA_MANAGER = 3;
    public static final int S_CONTRIBUTOR_CONFIG = 4;
    public static final int S_CONTRIBUTOR_LINK = 5;

    public static final int S_DUMMY_PACKET = 99;

    public static void init(IEventBus modBus) {
        LOCK.lock();
        CHANNEL.init(modBus);
    }

    public static void sendNoClip(ServerPlayer player, boolean enabled) {
        PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_NO_CLIP, player.registryAccess());
        packet.writeBoolean(enabled);
        packet.sendToPlayer(player);
        LogHelperBC.dev("Sending NoClip update to player: " + player + " Enabled: " + enabled);
    }

    public static void sendOpenPlayerAccessUI(ServerPlayer player, int windowID) {
        PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_PLAYER_ACCESS, player.registryAccess());
        packet.writeInt(windowID);
        packet.sendToPlayer(player);
    }

    public static void sendPlayerAccessUIUpdate(ServerPlayer player, Player target) {
        PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_PLAYER_ACCESS_UPDATE, player.registryAccess());
        packet.writeString(target.getGameProfile().getName());
        packet.writePos(target.blockPosition());
//        packet.writeInt(target.dimension.getId());
//        packet.sendToPlayer(player);
    }

    public static void sendPlayerAccessButton(int button, RegistryAccess registryAccess) {
        PacketCustom packet = new PacketCustom(CHANNEL_NAME, S_PLAYER_ACCESS_BUTTON, registryAccess);
        packet.writeByte(button);
        packet.sendToServer();
    }

    public static void sendIndexedMessage(ServerPlayer player, Component message, MessageSignature signature) {
        PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_INDEXED_MESSAGE, player.registryAccess());
        packet.writeTextComponent(message);
        packet.writeBytes(signature.bytes());
        packet.sendToPlayer(player);
    }

    public static void sendSound(Level level, int x, int y, int z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean distanceDelay) {
        sendSound(level, new BlockPos(x, y, z), sound, category, volume, pitch, distanceDelay);
    }

    public static void sendSound(Level level, Entity entity, SoundEvent sound, SoundSource category, float volume, float pitch, boolean distanceDelay) {
        sendSound(level, entity.blockPosition(), sound, category, volume, pitch, distanceDelay);
    }

    public static void sendSound(Level level, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch, boolean distanceDelay) {
        if (level instanceof ServerLevel serverLevel) {
            PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_PLAY_SOUND, level.registryAccess());
            packet.writePos(pos);
            packet.writeRegistryId(BuiltInRegistries.SOUND_EVENT, sound);
            packet.writeVarInt(category.ordinal());
            packet.writeFloat(volume);
            packet.writeFloat(pitch);
            packet.writeBoolean(distanceDelay);
            packet.sendToChunk(serverLevel, pos);
        }
    }

    public static void sendParticle(Level level, ParticleOptions particleData, Vector3 pos, Vector3 motion, boolean distanceOverride) {
        if (level instanceof ServerLevel serverLevel) {
            PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_SPAWN_PARTICLE, level.registryAccess());
//            packet.writeRegistryId(BuiltInRegistries.PARTICLE_TYPE, particleData.getType());
            ParticleTypes.STREAM_CODEC.encode(packet.toRegistryFriendlyByteBuf(), particleData);
            packet.writeVector(pos);
            packet.writeVector(motion);
            packet.writeBoolean(distanceOverride);
            packet.sendToChunk(serverLevel, pos.pos());
        }
    }

    /**
     * This is a custom entity spawn packet that removes the min/max velocity constraints.
     *
     * @param entity The entity being spawned.
     * @return A packet to return in {@link Entity#getAddEntityPacket()}
     */
    public static Packet<?> getEntitySpawnPacket(Entity entity, ServerEntity serverEntity, int ownerId) {
        PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_SPAWN_ENTITY, entity.registryAccess());
        packet.writeInt(entity.getId());
        packet.writeUUID(entity.getUUID());

        packet.writeDouble(serverEntity.getPositionBase().x());
        packet.writeDouble(serverEntity.getPositionBase().y());
        packet.writeDouble(serverEntity.getPositionBase().z());
        packet.writeByte((byte) Mth.floor(serverEntity.getLastSentXRot() * 256.0F / 360.0F));
        packet.writeByte((byte) Mth.floor(serverEntity.getLastSentYRot() * 256.0F / 360.0F));
        packet.writeByte((byte) (entity.getYHeadRot() * 256.0F / 360.0F));

        packet.writeRegistryId(BuiltInRegistries.ENTITY_TYPE, entity.getType());
        packet.writeVarInt(ownerId);

        Vec3 velocity = entity.getDeltaMovement();
        packet.writeFloat((float) velocity.x);
        packet.writeFloat((float) velocity.y);
        packet.writeFloat((float) velocity.z);
        return packet.toClientPacket();
    }

    public static Packet<?> sendEntityVelocity(Entity entity, boolean movement) {
        PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_ENTITY_VELOCITY, entity.registryAccess());
        packet.writeInt(entity.getId());
        packet.writeVec3f(entity.getDeltaMovement().toVector3f());
        packet.writeBoolean(movement);
        if (movement) {
            packet.writeFloat(entity.getXRot());
            packet.writeFloat(entity.getYRot());
            packet.writeBoolean(entity.onGround());
        }
        return packet.toClientPacket();
    }

    public static void sendOpenHudConfig(ServerPlayer player) {
        new PacketCustom(CHANNEL_NAME, C_OPEN_HUD_CONFIG, player.registryAccess()).sendToPlayer(player);
    }

    public static void sendMultiBlockDefinitions(ServerPlayer player, Map<ResourceLocation, MultiBlockDefinition> multiBlockMap) {
        PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_MULTI_BLOCK_DEFINITIONS, player.registryAccess());
        packet.writeVarInt(multiBlockMap.size());
        multiBlockMap.forEach((key, value) -> {
            packet.writeResourceLocation(key);
            packet.writeString(MultiBlockManager.GSON.toJson(value.getJson()));
        });
        packet.sendToPlayer(player);
    }

    public static void sendContributorConfigToServer(ContributorProperties props, RegistryAccess registryAccess) {
        PacketCustom packet = new PacketCustom(CHANNEL_NAME, S_CONTRIBUTOR_CONFIG, registryAccess);
        if (props.isContributor()) {
            props.getConfig().serialize(packet);
            packet.sendToServer();
        }
//        BrandonsCore.LOGGER.info("sendContributorConfigToServer");
    }

    public static PacketCustom contributorConfigToClient(ContributorProperties props, RegistryAccess registryAccess) {
        PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_CONTRIBUTOR_CONFIG, registryAccess);
        packet.writeUUID(props.getUserID());
        props.getConfig().serialize(packet);
        return packet;
    }

    public static void sendContribLinkToServer(RegistryAccess registryAccess) {
        new PacketCustom(CHANNEL_NAME, S_CONTRIBUTOR_LINK, registryAccess).sendToServer();
    }

    public static void sentToAllExcept(PacketCustom packet, Player exclude) {
        MinecraftServer server = exclude.getServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(exclude.getUUID())) {
                packet.sendToPlayer(player);
            }
        }
    }
}
