package com.brandon3055.brandonscore.blocks;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.api.IDataRetainingTile;
import com.brandon3055.brandonscore.api.event.TileBCoreInitEvent;
import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.brandonscore.api.power.IOTracker;
import com.brandon3055.brandonscore.api.power.OPStorage;
import com.brandon3055.brandonscore.capability.CapabilityOP;
import com.brandon3055.brandonscore.inventory.ContainerBCTile;
import com.brandon3055.brandonscore.inventory.TileItemStackHandler;
import com.brandon3055.brandonscore.lib.IRSSwitchable;
import com.brandon3055.brandonscore.lib.IRSSwitchable.RSMode;
import com.brandon3055.brandonscore.lib.datamanager.*;
import com.brandon3055.brandonscore.network.BCoreNetwork;
import com.brandon3055.brandonscore.utils.EnergyUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.brandon3055.brandonscore.lib.datamanager.DataFlags.*;

/**
 * Created by brandon3055 on 26/3/2016.
 * Base tile entity class for all tile entities
 */
public class TileBCore extends BlockEntity implements IDataManagerProvider, IDataRetainingTile, Nameable {
    public static final Logger LOGGER = LogManager.getLogger();

    protected boolean playerAccessTracking = false;
    protected TileCapabilityManager capManager = new TileCapabilityManager(this);
    protected TileDataManager<TileBCore> dataManager = new TileDataManager<>(this);
    private Map<Integer, BiConsumer<MCDataInput, ServerPlayer>> serverPacketHandlers = new HashMap<>();
    protected Map<String, INBTSerializable<CompoundTag>> savedItemDataObjects = new HashMap<>();
    protected Map<String, INBTSerializable<CompoundTag>> savedDataObjects = new HashMap<>();
    private Map<Integer, Consumer<MCDataInput>> clientPacketHandlers = new HashMap<>();

    private boolean debugOutputEnabled = false;
    private ManagedBool debugEnabled = null;

    private List<Runnable> tickables = new ArrayList<>();
    private ManagedEnum<RSMode> rsControlMode = this instanceof IRSSwitchable ? register(new ManagedEnum<>("rs_mode", RSMode.ALWAYS_ACTIVE, SAVE_BOTH_SYNC_TILE, CLIENT_CONTROL)) : null;
    private ManagedBool rsPowered = this instanceof IRSSwitchable ? register(new ManagedBool("rs_powered", false, SAVE_NBT_SYNC_TILE, TRIGGER_UPDATE)) : null;
    private String customName = "";
    private Set<Player> accessingPlayers = new HashSet<>();
    private int tick = 0;

    public TileBCore(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        NeoForge.EVENT_BUS.post(new TileBCoreInitEvent(this));
    }

    //region Data Manager

    @Override
    public TileDataManager getDataManager() {
        return dataManager;
    }

    public TileCapabilityManager getCapManager() {
        return capManager;
    }

    protected static <T, BE extends TileBCore> void capability(RegisterCapabilitiesEvent event, Supplier<BlockEntityType<BE>> type, BlockCapability<T, Direction> capability) {
        event.registerBlockEntity(capability, type.get(), (tile, side) -> tile.getCapManager().getCapability(capability, side));
    }

    protected static <BE extends TileBCore> void energyCapability(RegisterCapabilitiesEvent event, Supplier<BlockEntityType<BE>> type) {
        event.registerBlockEntity(CapabilityOP.BLOCK, type.get(), (tile, side) -> tile.getCapManager().getCapability(CapabilityOP.BLOCK, side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type.get(), (tile, side) -> tile.getCapManager().getCapability(CapabilityOP.BLOCK, side));
    }

    /**
     * Convenience method for dataManager.register();
     */
    public <M extends IManagedData> M register(M managedData) {
        return dataManager.register(managedData);
    }

    /**
     * super.tick() must be called from your update method in order for Data Manager synchronization to work..
     */
    public void tick() {
        tickables.forEach(Runnable::run);
        detectAndSendChanges(false);
        tick++;
    }

    public void detectAndSendChanges(boolean containerListeners) {
        if (level != null && !level.isClientSide) {
            if (containerListeners) {
                dataManager.detectAndSendChangesToListeners(getAccessingPlayers());
                capManager.detectAndSendChangesToListeners(getAccessingPlayers());
            } else {
                dataManager.detectAndSendChanges();
                capManager.detectAndSendChanges();
            }
        }
    }

    public int getAccessDistanceSq() {
        return 64;
    }

    //endregion

    //region Packets
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag compound = super.getUpdateTag(provider);
        dataManager.writeSyncNBT(provider, compound);
        writeExtraNBT(provider, compound);
        return compound;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        dataManager.readSyncNBT(provider, pkt.getTag());
        readExtraNBT(provider, pkt.getTag());
    }

    public PacketCustom createServerBoundPacket(int id) {
        Player player = BrandonsCore.proxy.getClientPlayer();
        if (player != null) {
            AbstractContainerMenu container = player.containerMenu;
            if (container instanceof ContainerBCTile && ((ContainerBCTile<?>) container).tile == this) {
                PacketCustom packet = ((ContainerBCTile<?>) container).createServerBoundPacket(BCoreNetwork.S_CONTAINER_MESSAGE);
                packet.writeByte((byte) id);
                return packet;
            }
        }
        return new PacketCustom(BCoreNetwork.CHANNEL_NAME, BCoreNetwork.S_DUMMY_PACKET, getLevel().registryAccess());
    }

    /**
     * Send a data packet to the server, Supply a consumer to write the data you want to send
     * Note: This packet now goes through the container for this block for security.
     * Meaning this tile must have an associated container, and that container must be open for this to work
     */
    public void sendPacketToServer(Consumer<MCDataOutput> writer, int id) {
        PacketCustom packet = createServerBoundPacket(id);
        writer.accept(packet);
        packet.sendToServer();
    }

    /**
     * Override this method to receive data from the server via sendPacketToServer
     */
    public void receivePacketFromClient(MCDataInput data, ServerPlayer client, int id) {
        if (serverPacketHandlers.containsKey(id)) {
            serverPacketHandlers.get(id).accept(data, client);
        }
    }

    public PacketCustom createClientBoundPacket(int id) {
        PacketCustom packet = new PacketCustom(BCoreNetwork.CHANNEL_NAME, BCoreNetwork.C_TILE_MESSAGE, getLevel().registryAccess());
        packet.writePos(worldPosition);
        packet.writeByte((byte) id);
        return packet;
    }

    /**
     * Create a packet to send to the client
     */
    public PacketCustom sendPacketToClient(Consumer<MCDataOutput> writer, int id) {
        PacketCustom packet = createClientBoundPacket(id);
        writer.accept(packet);
        return packet;
    }

    public void sendPacketToClient(ServerPlayer player, Consumer<MCDataOutput> writer, int id) {
        sendPacketToClient(writer, id).sendToPlayer(player);
    }

    public void sendPacketToClients(Collection<Player> players, Consumer<MCDataOutput> writer, int id) {
        PacketCustom packet = createClientBoundPacket(id);
        writer.accept(packet);
        sendPacketToClients(players, packet);
    }

    public void sendPacketToClients(Collection<Player> players, PacketCustom packet) {
        players.stream().filter(e -> e instanceof ServerPlayer).map(e -> (ServerPlayer) e).forEach(packet::sendToPlayer);
    }

    public void sendPacketToChunk(Consumer<MCDataOutput> writer, int id) {
        sendPacketToClient(writer, id).sendToChunk(this);
    }

    /**
     * Override this method to receive data from the client via sendPacketToClient
     */
    public void receivePacketFromServer(MCDataInput data, int id) {
        if (clientPacketHandlers.containsKey(id)) {
            clientPacketHandlers.get(id).accept(data);
        }
    }

    /**
     * Sets a client side packet handler to handle packets sent from the server with the specified id
     *
     * @param packetId packet id
     * @param handler  the handler for this packet
     */
    public void setClientSidePacketHandler(int packetId, Consumer<MCDataInput> handler) {
        this.clientPacketHandlers.put(packetId, handler);
    }

    /**
     * Sets a server side packet handler to handle packets sent from the client with the specified id
     *
     * @param packetId packet id
     * @param handler  the handler for this packet
     */
    public void setServerSidePacketHandler(int packetId, BiConsumer<MCDataInput, ServerPlayer> handler) {
        this.serverPacketHandlers.put(packetId, handler);
    }

    //endregion

    //region Helper Functions.

    public void updateBlock() {
        BlockState state = level.getBlockState(getBlockPos());
        level.sendBlockUpdated(getBlockPos(), state, state, 3);
    }

    public void dirtyBlock() {
        LevelChunk chunk = level.getChunkAt(getBlockPos());
        chunk.setUnsaved(true);
    }

    /**
     * Adds an item to the 'tickables' list. Every item in this list will be called every tick via the tiles update method.
     * Note: in order for this to work the tile must be ticking and call super in {@link #tick()}
     *
     * @param runnable The runnable to add
     */
    public <T extends Runnable> T addTickable(T runnable) {
        tickables.add(runnable);
        return runnable;
    }

    public boolean removeTickable(Runnable runnable) {
        return tickables.remove(runnable);
    }

    //endregion

    //region Save/Load

    /**
     * These methods replace the methods from IDataRetainerTile but they are
     * now ONLY used to read and write data to and from an itemstack<br>
     * Note: if you wish to add additional data to the item then override this, Call super to get a data tag,
     * Write your data to said tag and finally return said tag.
     */
    @Override
    public void writeToItemStack(HolderLookup.Provider provider, CompoundTag nbt, boolean willHarvest) {
        dataManager.writeToStackNBT(provider, nbt);
        savedItemDataObjects.forEach((tagName, serializable) -> nbt.put(tagName, serializable.serializeNBT(provider)));
        CompoundTag capTags = capManager.serialize(true);
        if (!capTags.isEmpty()) {
            nbt.put("bc_caps", capTags);
        }
        writeExtraTileAndStack(provider, nbt);
    }


    @Override
    public void readFromItemStack(HolderLookup.Provider provider, CompoundTag nbt) {
        dataManager.readFromStackNBT(provider, nbt);
        savedItemDataObjects.forEach((tagName, serializable) -> serializable.deserializeNBT(provider, nbt.getCompound(tagName)));
        if (nbt.contains("bc_caps")) {
            capManager.deserialize(provider, nbt.getCompound("bc_caps"));
        }
        readExtraTileAndStack(provider, nbt);
    }

    /**
     * Write any extra data that needs to be saved to NBT that is not saved via a syncable field.
     * This data is also synced to the client via getUpdateTag and getUpdatePacket.
     * Note: This will not save data to the item when the block is harvested.<br>
     * For that you need to override read and writeToStack just be sure to pay attention to the doc for those.
     */
    public void writeExtraNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        CompoundTag capTags = capManager.serialize(false);
        if (!capTags.isEmpty()) {
            nbt.put("bc_caps", capTags);
        }

        if (!customName.isEmpty()) {
            nbt.putString("custom_name", customName);
        }

        savedDataObjects.forEach((tagName, serializable) -> nbt.put(tagName, serializable.serializeNBT(provider)));
        writeExtraTileAndStack(provider, nbt);
    }

    public void readExtraNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt.contains("bc_caps")) {
            capManager.deserialize(provider, nbt.getCompound("bc_caps"));
        }

        if (nbt.contains("custom_name", 8)) {
            customName = nbt.getString("custom_name");
        }

        savedDataObjects.forEach((tagName, serializable) -> serializable.deserializeNBT(provider, nbt.getCompound(tagName)));
        readExtraTileAndStack(provider, nbt);
    }

    /**
     * Convenience method that is called by both
     * {@link #writeExtraNBT(HolderLookup.Provider, CompoundTag)} and
     * {@link #writeToItemStack(HolderLookup.Provider, CompoundTag, boolean)}
     */
    public void writeExtraTileAndStack(HolderLookup.Provider provider, CompoundTag nbt) {}

    /**
     * Convenience method that is called by both
     * {@link #readExtraNBT(HolderLookup.Provider, CompoundTag)} and
     * {@link #readFromItemStack(HolderLookup.Provider, CompoundTag)}
     */
    public void readExtraTileAndStack(HolderLookup.Provider provider, CompoundTag nbt) {}

    @Override
    protected final void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        dataManager.writeToNBT(provider, nbt);
        writeExtraNBT(provider, nbt);
    }

    @Override
    protected final void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        dataManager.readFromNBT(provider, nbt);
        readExtraNBT(provider, nbt);
        onTileLoaded();
    }

    /**
     * Called immediately after all NBT is loaded. World may be null at this point
     */
    public void onTileLoaded() {
    }

    /**
     * Allows you to add raw INBTSerializable data objects too be saved and loaded directly from the tile's NBT.
     * This saves and loads the data server side. Its also sent in the default chunk sync packet.
     *
     * @param tagName    the name to use when saving this object to the tile's NBT
     * @param dataObject the serializable data object.
     */
    public void setSavedDataObject(String tagName, INBTSerializable<CompoundTag> dataObject) {
        this.savedDataObjects.put(tagName, dataObject);
    }

    public void setItemSavedDataObject(String tagName, INBTSerializable<CompoundTag> dataObject) {
        this.savedItemDataObjects.put(tagName, dataObject);
    }

    //endregion

    //region EnergyHelpers

    public long sendEnergyToAll(long maxPerTarget, long maxAvailable) {
        long i = 0;
        for (Direction direction : Direction.values()) {
            i += sendEnergyTo(Math.min(maxPerTarget, maxAvailable - i), direction);
        }
        return i;
    }

    public long sendEnergyTo(long maxSend, Direction side) {
        if (maxSend == 0) {
            return 0;
        }

        BlockEntity tile = level.getBlockEntity(worldPosition.relative(side));
        if (tile != null) {
            return EnergyUtils.insertEnergy(tile, maxSend, side.getOpposite(), false);
        }
        return 0;
    }

    public static long sendEnergyTo(LevelReader world, BlockPos pos, long maxSend, Direction side) {
        if (maxSend == 0) {
            return 0;
        }

        BlockEntity tile = world.getBlockEntity(pos.relative(side));
        if (tile != null) {
            return EnergyUtils.insertEnergy(tile, maxSend, side.getOpposite(), false);
        }
        return 0;
    }

    public static long sendEnergyToAll(LevelReader world, BlockPos pos, long maxPerTarget, long maxAvailable) {
        long i = 0;
        for (Direction direction : Direction.values()) {
            i += sendEnergyTo(world, pos, Math.min(maxPerTarget, maxAvailable - i), direction);
        }
        return i;
    }

    /**
     * Adds an io tracker to the specified storage and ensures the tracker is updated every tick.
     * Note: for updating to work this tile must be ticking and call super in {@link #tick()}
     *
     * @param storage The storage to add an IO tracker to.
     */
    public void installIOTracker(OPStorage storage) {
        storage.setIOTracker(addTickable(new IOTracker()));
    }

    /**
     * This method configures the specified slot in the specified item handler as an energy item slot.
     * The item in this slot will be automatically charged or discharged depending on whether chargeItem is true or false.
     * <p>
     * If the IItemHandler is an instance of {@link TileItemStackHandler} then this will automatically add a slot validator limiting
     * the slot to items that can receive/provide energy.
     *
     * @param itemHandler The item handler.
     * @param slot        The slot in the item handler.
     * @param storage     The storage to transfer energy into or out of.
     * @param chargeItem  A managed boolean that controls whether the item is being charged or discharged.
     */
    public void setupPowerSlot(IItemHandler itemHandler, int slot, IOPStorage storage, ManagedBool chargeItem) {
        setupPowerSlot(itemHandler, slot, storage, chargeItem::get);
    }

    /**
     * This method configures the specified slot in the specified item handler as an energy item slot.
     * The item in this slot will be automatically charged or discharged depending on whether chargeItem is true or false.
     * <p>
     * If the IItemHandler is an instance of {@link TileItemStackHandler} then this will automatically add a slot validator limiting
     * the slot to items that can receive/provide energy.
     *
     * @param itemHandler The item handler.
     * @param slot        The slot in the item handler.
     * @param storage     The storage to transfer energy into or out of.
     * @param chargeItem  If True the item will be charged otherwise it will be discharged.
     */
    public void setupPowerSlot(IItemHandler itemHandler, int slot, IOPStorage storage, boolean chargeItem) {
        setupPowerSlot(itemHandler, slot, storage, () -> chargeItem);
    }

    private void setupPowerSlot(IItemHandler itemHandler, int slot, IOPStorage storage, Supplier<Boolean> chargeItem) {
        if (itemHandler instanceof TileItemStackHandler) {
            ((TileItemStackHandler) itemHandler).setSlotValidator(slot, stack -> (chargeItem.get() ? EnergyUtils.canReceiveEnergy(stack) : EnergyUtils.canExtractEnergy(stack)));
        }
        if (EffectiveSide.get().isServer()) {
            addTickable(() -> {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    if (chargeItem.get()) {
                        EnergyUtils.transferEnergy(storage, stack);
                    } else {
                        EnergyUtils.transferEnergy(stack, storage);
                    }
                }
            });
        }
    }


    //endregion

    //Other

    /**
     * Only works on ticking tiles that call super.update()
     *
     * @return an internal tick timer specific to this tile
     */
    public int getTime() {
        return tick;
    }

    /**
     * Only works on ticking tiles that call super.update()
     *
     * @return true once every 'tickInterval' based on the tiles internal timer.
     */
    public boolean onInterval(int tickInterval) {
        return tick % tickInterval == 0;
    }

    public RSMode getRSMode() {
        if (!(this instanceof IRSSwitchable)) {
            throw new IllegalStateException("Tile does not implement IRSSwitchable");
        }
        return rsControlMode.get();
    }

    public void setRSMode(RSMode mode) {
        if (!(this instanceof IRSSwitchable)) {
            throw new IllegalStateException("Tile does not implement IRSSwitchable");
        }
        rsControlMode.set(mode);
    }

    public void cycleRSMode(boolean reverse) {
        rsControlMode.set(rsControlMode.get().next(reverse));
    }

    public void onNeighborChange(BlockPos neighbor) {
        if (this instanceof IRSSwitchable) {
            boolean lastSignal = rsPowered.get();
            rsPowered.set(level.hasNeighborSignal(worldPosition));
            if (rsPowered.get() != lastSignal) {
                onSignalChange(rsPowered.get());
            }
        }
    }

    public void onSignalChange(boolean newSignal) {

    }

    /**
     * If this tile implements {@link IRSSwitchable} this method can be used to check if the tile is currently allowed to run.
     * This takes the current redstone state as well as the current control mode into consideration.
     *
     * @return true if the current RS control mode allows the tile to run given its current redstone state.
     */
    public boolean isTileEnabled() {
        if (this instanceof IRSSwitchable) {
            return rsControlMode.get().canRun(rsPowered.get());
        }

        return true;
    }

    public boolean hasRSSignal() {
        return level.hasNeighborSignal(getBlockPos());
    }

    public int getRSSignal() {
        return level.getBestNeighborSignal(getBlockPos());
    }

    @Override
    public Component getName() {
        if (hasCustomName()) {
            return Component.literal(customName);
        }

        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public boolean hasCustomName() {
        return !customName.isEmpty();
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return customName.isEmpty() ? null : getName();
    }

    public Component getDisplayName() {
        return getName();
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    /**
     * If enabled a list of players currently accessing this tiles container will be available via {@link #getAccessingPlayers()}
     *
     * @param playerAccessTracking enable tracking
     */
    public void enablePlayerAccessTracking(boolean playerAccessTracking) {
        this.playerAccessTracking = playerAccessTracking;
    }

    /**
     * @return a list of players currently accessing this tile's container.
     * playerAccessTracking must be enabled in this tile's constructor in order for this to work.
     */
    public Set<Player> getAccessingPlayers() {
        accessingPlayers.removeIf(e -> !(e.containerMenu instanceof ContainerBCTile<?> container) || container.tile != this); //Clean up set
        return accessingPlayers;
    }

    public void onPlayerOpenContainer(Player player) {
        accessingPlayers.add(player);
    }

    public void onPlayerCloseContainer(Player player) {
        accessingPlayers.remove(player);
        accessingPlayers.removeIf(e -> !(e.containerMenu instanceof ContainerBCTile<?> container) || container.tile != this); //Clean up set
    }

    public int posSeed() {
        return (int) worldPosition.asLong();
    }

    //Debug

    /**
     * Call from tile constructor to enable debug-ability
     */
    protected void enableTileDebug() {
        register(debugEnabled = new ManagedBool("tile.debugging.enabled", false, SYNC_TILE));
    }

    public boolean toggleDebugOutput(Player player) {
        if (debugEnabled == null) {
            return false;
        } else {
            debugEnabled.invert();
            player.sendSystemMessage(Component.literal("Debug is now " + (debugEnabled.get() ? "Enabled (Check Server/Client console)" : "Disabled")));
        }
        return true;
    }

    public void debug(Object text) {
        if (debugEnabled()) {
            LOGGER.info("TileDebug:" + getBlockPos() + ", " + getLevel().dimension().location() + ": " + text);
        }
    }

    public boolean debugEnabled() {
        return debugEnabled != null && debugEnabled.get();
    }
}
