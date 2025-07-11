package com.brandon3055.brandonscore.worldentity;

import com.brandon3055.brandonscore.utils.LogHelperBC;
import com.google.common.collect.ImmutableList;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.brandon3055.brandonscore.BrandonsCore.MODID;

/**
 * Created by brandon3055 on 15/12/20
 */
public class WorldEntityHandler {
    private static final CrashLock LOCK = new CrashLock("Already Initialized.");

    public static final ResourceKey<Registry<WorldEntityType<?>>> ENTITY_TYPE = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MODID, "world_entity"));
    public static Registry<WorldEntityType<?>> REGISTRY;
    private static final Map<UUID, WorldEntity> ID_ENTITY_MAP = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<WorldEntity>> WORLD_ENTITY_MAP = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<ITickableWorldEntity>> TICKING_ENTITY_MAP = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<WorldEntity>> ADDED_WORLD_ENTITIES = new HashMap<>();

    public static void createRegistry(NewRegistryEvent event) {
        REGISTRY = event.create(new RegistryBuilder<>(ENTITY_TYPE)
                .sync(false)
        );
    }

    public static void init(IEventBus modBus) {
        LOCK.lock();
        modBus.addListener(WorldEntityHandler::createRegistry);

        NeoForge.EVENT_BUS.addListener(WorldEntityHandler::worldLoad);
        NeoForge.EVENT_BUS.addListener(WorldEntityHandler::worldUnload);
        NeoForge.EVENT_BUS.addListener(WorldEntityHandler::onServerStop);
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre event) -> WorldEntityHandler.worldTick(event));
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> WorldEntityHandler.worldTick(event));
    }

    public static void worldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        ServerLevel world = (ServerLevel) event.getLevel();
        ResourceKey<Level> key = world.dimension();

        //If the world was unloaded properly then this should always be null. But better safe
        List<WorldEntity> oldEntities = WORLD_ENTITY_MAP.remove(key);
        TICKING_ENTITY_MAP.remove(key);
        if (oldEntities != null) {
            LogHelperBC.warn("Detected stray world entities for world " + key + ". These should have been removed when the world unloaded.");
            oldEntities.forEach(e -> ID_ENTITY_MAP.remove(e.getUniqueID()));
            WORLD_ENTITY_MAP.remove(key);
        }

        WorldEntitySaveData data = world.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldEntitySaveData::new, WorldEntitySaveData::load), WorldEntitySaveData.FILE_ID);
        data.setSaveCallback(() -> handleSave(data, key));
        for (WorldEntity entity : data.getEntities()) {
            addWorldEntity(world, entity);
        }
    }

    private static void handleSave(WorldEntitySaveData data, ResourceKey<Level> key) {
        List<WorldEntity> worldEntities = WORLD_ENTITY_MAP.get(key);
        data.updateEntities(worldEntities);
    }

    public static void worldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        ServerLevel world = (ServerLevel) event.getLevel();
        ResourceKey<Level> key = world.dimension();
        TICKING_ENTITY_MAP.remove(key);
        List<WorldEntity> removed = WORLD_ENTITY_MAP.get(key);
        if (removed != null) {
            removed.forEach(e -> ID_ENTITY_MAP.remove(e.getUniqueID()));
        }
    }

    public static void onServerStop(ServerStoppedEvent event) {
        WORLD_ENTITY_MAP.clear();
        TICKING_ENTITY_MAP.clear();
        ID_ENTITY_MAP.clear();
    }

    public static void worldTick(LevelTickEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ResourceKey<Level> key = level.dimension();

        //Clear dead entities
        ID_ENTITY_MAP.entrySet().removeIf(entry -> {
            WorldEntity entity = entry.getValue();
            if (entity.isRemoved()) {
                ResourceKey<Level> removeKey = entity.level.dimension();
                if (WORLD_ENTITY_MAP.containsKey(removeKey)) {
                    WORLD_ENTITY_MAP.get(removeKey).remove(entity);
                }
                if (entity instanceof ITickableWorldEntity && TICKING_ENTITY_MAP.containsKey(removeKey)) {
                    TICKING_ENTITY_MAP.get(removeKey).remove(entity);
                }
                return true;
            }
            return false;
        });

        //Tick Tickable Entities
        if (TICKING_ENTITY_MAP.containsKey(key)) {
            TICKING_ENTITY_MAP.get(key).forEach(e -> {
                if (e.onTickEnd() == (event instanceof LevelTickEvent.Post)) {
                    e.tick();
                }
            });
        }

        //Add New Entities
        if (event instanceof LevelTickEvent.Post && ADDED_WORLD_ENTITIES.containsKey(key)) {
            List<WorldEntity> newEntities = ADDED_WORLD_ENTITIES.get(key);
            if (!newEntities.isEmpty()) {
                List<WorldEntity> worldEntities = WORLD_ENTITY_MAP.computeIfAbsent(key, e -> new ArrayList<>());
                List<ITickableWorldEntity> worldTickingEntities = TICKING_ENTITY_MAP.computeIfAbsent(key, e -> new ArrayList<>());
                for (WorldEntity entity : newEntities) {
                    if (entity.isRemoved()) {
                        worldEntities.remove(entity);
                        if (entity instanceof ITickableWorldEntity) {
                            worldTickingEntities.remove(entity);
                        }
                        continue;
                    }
                    ID_ENTITY_MAP.put(entity.getUniqueID(), entity);
                    if (!worldEntities.contains(entity)) {
                        worldEntities.add(entity);
                    }
                    if (entity instanceof ITickableWorldEntity && !worldTickingEntities.contains(entity)) {
                        worldTickingEntities.add((ITickableWorldEntity) entity);
                    }
                    if (entity.getLevel() != level) {
                        entity.setLevel(level);
                    }
                    entity.onLoad();
                }
            }
            ADDED_WORLD_ENTITIES.remove(key);
        }
    }

    public static void addWorldEntity(Level world, WorldEntity entity) {
        if (!(world instanceof ServerLevel)) return;
        ResourceKey<Level> key = world.dimension();
        ADDED_WORLD_ENTITIES.computeIfAbsent(key, e -> new ArrayList<>()).add(entity);
        entity.setLevel(world);
    }

    @Nullable
    public static WorldEntity getWorldEntity(Level world, UUID id) {
        WorldEntity entity = ID_ENTITY_MAP.get(id);
        if (entity == null && ADDED_WORLD_ENTITIES.containsKey(world.dimension())) {
            entity = ADDED_WORLD_ENTITIES.get(world.dimension()).stream().filter(e -> e.getUniqueID().equals(id)).findAny().orElse(null);
        }
        return entity;
    }

    public static List<WorldEntity> getWorldEntities() {
        if (ADDED_WORLD_ENTITIES.isEmpty()) {
            return ImmutableList.copyOf(ID_ENTITY_MAP.values());
        }
        Set<WorldEntity> set = new HashSet<>();
        set.addAll(ID_ENTITY_MAP.values());
        set.addAll(ADDED_WORLD_ENTITIES.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
        return ImmutableList.copyOf(set);
    }

    protected static void onEntityRemove(WorldEntity entity) {
        ResourceKey<Level> key = entity.getLevel().dimension();
        if (ADDED_WORLD_ENTITIES.containsKey(key)) {
            ADDED_WORLD_ENTITIES.get(key).remove(entity);
        }
    }
}
