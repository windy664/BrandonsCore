package com.brandon3055.brandonscore.blocks;

import com.brandon3055.brandonscore.lib.IMCDataSerializable;
import com.brandon3055.brandonscore.lib.IValueHashable;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Created by brandon3055 on 18/12/19.
 */
public class SerializationFlags<D extends INBTSerializable<CompoundTag>> {
    protected final String tagName;
    protected final D serializableInstance;
    protected final Level level;
    protected Object lastData;

    protected boolean saveTile = false;
    protected boolean saveItem = false;
    protected boolean syncTile = false;
    protected boolean syncContainer = false;

    public SerializationFlags(String tagName, D serializableInstance, Level level) {
        this.tagName = tagName;
        this.serializableInstance = serializableInstance;
        this.level = level;

        if (serializableInstance instanceof IValueHashable<?> hashable) {
            lastData = hashable.getValueHash();
            syncContainer = true;
        } else {
            lastData = serializableInstance.serializeNBT(level.registryAccess());
        }
    }

    public D getData() {
        return serializableInstance;
    }

    /**
     * Enable saving to item when broken.
     */
    public SerializationFlags<D> saveItem() {
        this.saveItem = true;
        return this;
    }

    /**
     * Enable saving to tile data.
     */
    public SerializationFlags<D> saveTile() {
        this.saveTile = true;
        return this;
    }

    /**
     * Enable saving to both item and tile data.
     * */
    public SerializationFlags<D> saveBoth() {
        saveItem();
        saveTile();
        return this;
    }

    /**
     * Enable syncing via tile entity.
     * You can also implement {@link IMCDataSerializable} on your data instance to improve sync efficiency.
     */
    public SerializationFlags<D> syncTile() {
        this.syncTile = true;
        return this;
    }

    /**
     * Enable syncing via container.
     * Is automatically enabled for any serializable that also implements {@link IValueHashable}
     * You can also implement {@link IMCDataSerializable} on your data instance to improve sync efficiency.
     */
    public SerializationFlags<D> syncContainer() {
        this.syncContainer = true;
        return this;
    }

    protected boolean hasChanged(boolean reset) {
        if (serializableInstance instanceof IValueHashable<?> hashable) {
            if (!hashable.checkValueHash(lastData)) {
                if (reset) {
                    lastData = hashable.getValueHash();
                }
                return true;
            }
        } else {
            if (!serializableInstance.serializeNBT(level.registryAccess()).equals(lastData)) {
                if (reset) {
                    lastData = serializableInstance.serializeNBT(level.registryAccess());
                }
                return true;
            }
        }

        return false;
    }
}
