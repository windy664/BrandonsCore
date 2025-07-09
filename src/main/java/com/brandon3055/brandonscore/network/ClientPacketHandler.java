package com.brandon3055.brandonscore.network;

import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.blocks.TileBCore;
import com.brandon3055.brandonscore.handlers.BCEventHandler;
import com.brandon3055.brandonscore.handlers.contributor.ContributorHandler;
import com.brandon3055.brandonscore.lib.datamanager.IDataManagerProvider;
import com.brandon3055.brandonscore.multiblock.MultiBlockManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

public class ClientPacketHandler implements ICustomPacketHandler.IClientPacketHandler {

    @Override
    public void handlePacket(PacketCustom packet, Minecraft mc) {
        switch (packet.getType()) {
            case BCoreNetwork.C_TILE_DATA_MANAGER -> {
                BlockPos pos = packet.readPos();
                if (mc.level.getBlockEntity(pos) instanceof IDataManagerProvider tile) {
                    tile.getDataManager().receiveSyncData(packet);
                }
            }
            case BCoreNetwork.C_TILE_MESSAGE -> {
                BlockPos pos = packet.readPos();
                if (mc.level.getBlockEntity(pos) instanceof TileBCore tile) {
                    int id = packet.readByte() & 0xFF;
                    tile.receivePacketFromServer(packet, id);
                }
            }
            case BCoreNetwork.C_NO_CLIP -> {
                boolean enable = packet.readBoolean();
                if (enable) {
                    BCEventHandler.noClipPlayers.add(mc.player.getUUID());
                } else {
                    BCEventHandler.noClipPlayers.remove(mc.player.getUUID());
                }
            }
            case BCoreNetwork.C_PLAYER_ACCESS_UPDATE -> {
                //GuiPlayerAccess gui = mc.screen instanceof GuiPlayerAccess ? (GuiPlayerAccess) mc.screen : null;
                //if (gui != null) {
                //    gui.name = packet.readString();
                //    gui.pos = packet.readPos();
                //    gui.dimension = packet.readInt();
                //}
            }
            case BCoreNetwork.C_INDEXED_MESSAGE -> BrandonsCore.proxy.sendIndexedMessage(mc.player, packet.readTextComponent(), new MessageSignature(packet.readBytes()));
            case BCoreNetwork.C_TILE_CAP_DATA -> {
                BlockPos pos = packet.readPos();
                if (mc.level.getBlockEntity(pos) instanceof TileBCore tile) {
                    tile.getCapManager().receiveCapSyncData(packet);
                }
            }
            case BCoreNetwork.C_PLAY_SOUND -> handlePlaySound(packet, mc);
            case BCoreNetwork.C_SPAWN_ENTITY -> handleEntitySpawn(packet, mc);
            case BCoreNetwork.C_SPAWN_PARTICLE -> handleParticleSpawn(packet, mc);
            case BCoreNetwork.C_ENTITY_VELOCITY -> handleEntityVelocity(packet, mc);
            //case BCoreNetwork.C_OPEN_HUD_CONFIG -> mc.setScreen(new HudConfigGui());
            case BCoreNetwork.C_MULTI_BLOCK_DEFINITIONS -> MultiBlockManager.receiveDefinitionsFromServer(packet);
            case BCoreNetwork.C_CONTRIBUTOR_CONFIG -> ContributorHandler.handleSettingsFromServer(packet);
        }
    }

    private static void handlePlaySound(PacketCustom packet, Minecraft mc) {
        if (mc.level == null) return;
        BlockPos pos = packet.readPos();
        SoundEvent sound = packet.readRegistryId();
        SoundSource category = SoundSource.values()[packet.readVarInt()];
        float volume = packet.readFloat();
        float pitch = packet.readFloat();
        boolean distanceDelay = packet.readBoolean();
        mc.level.playLocalSound(pos, sound, category, volume, pitch, distanceDelay);
    }

//    PacketCustom packet = new PacketCustom(CHANNEL_NAME, C_SPAWN_ENTITY, entity.registryAccess());
//        packet.writeInt(entity.getId());
//        packet.writeUUID(entity.getUUID());
//
//        packet.writeDouble(serverEntity.getPositionBase().x());
//        packet.writeDouble(serverEntity.getPositionBase().y());
//        packet.writeDouble(serverEntity.getPositionBase().z());
//        packet.writeByte((byte) Mth.floor(serverEntity.getLastSentXRot() * 256.0F / 360.0F));
//        packet.writeByte((byte) Mth.floor(serverEntity.getLastSentYRot() * 256.0F / 360.0F));
//        packet.writeByte((byte) (entity.getYHeadRot() * 256.0F / 360.0F));
//
//        packet.writeRegistryId(BuiltInRegistries.ENTITY_TYPE, entity.getType());
//        packet.writeVarInt(ownerId);
//
//    Vec3 velocity = entity.getDeltaMovement();
//        packet.writeFloat((float) velocity.x);
//        packet.writeFloat((float) velocity.y);
//        packet.writeFloat((float) velocity.z);

    private static void handleEntitySpawn(PacketCustom packet, Minecraft mc) {
        if (mc.level == null) {
            return;
        }
        int entityID = packet.readInt();
        UUID uuid = packet.readUUID();

        double posX = packet.readDouble();
        double posY = packet.readDouble();
        double posZ = packet.readDouble();
        byte xRot = packet.readByte();
        byte yRot = packet.readByte();
        byte headYRot = packet.readByte();

        EntityType<?> type = packet.readRegistryId();//ForgeRegistries.ENTITY_TYPES.byId(packet.readVarInt());
        int ownerId = packet.readVarInt();
        Vec3 velocity = new Vec3(packet.readFloat(), packet.readFloat(), packet.readFloat());

        Entity entity = type.create(mc.level);
        if (entity == null) {
            return;
        }

        //This is a hack, but meh. Should work.
        if (entity instanceof Projectile projectile && ownerId != 0) {
            Entity e = mc.level.getEntity(ownerId);
            if (e != null) {
                projectile.setOwner(entity);
            }
        }

        entity.setDeltaMovement(velocity);
        entity.syncPacketPositionCodec(posX, posY, posZ);
        entity.moveTo(posX, posY, posZ);
        entity.setXRot(xRot);
        entity.setYRot(yRot);
        entity.setId(entityID);
        entity.setUUID(uuid);
        mc.level.addEntity(entity);
    }

    private static void handleEntityVelocity(PacketCustom packet, Minecraft mc) {
        if (mc.level == null) {
            return;
        }
        int entityID = packet.readInt();
        Entity entity = mc.level.getEntity(entityID);
        if (entity != null) {
            Vector3f motion = packet.readVec3f();
            entity.lerpMotion(motion.x(), motion.y(), motion.z());
            if (packet.readBoolean()) {
                entity.setXRot(packet.readFloat());
                entity.setYRot(packet.readFloat());
                entity.setOnGround(packet.readBoolean());
            }
        }
    }

    private static void handleParticleSpawn(PacketCustom packet, Minecraft mc) {
        if (mc.level == null) {
            return;
        }
//        ParticleType<?> type = packet.readRegistryId();
        ParticleOptions data = ParticleTypes.STREAM_CODEC.decode(packet.toRegistryFriendlyByteBuf());
        Vector3 pos = packet.readVector();
        Vector3 motion = packet.readVector();
        boolean distanceOverride = packet.readBoolean();
        ;
        mc.level.addParticle(data, distanceOverride, pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
    }
}