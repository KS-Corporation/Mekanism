package mekanism.common.tile.interfaces;

import net.minecraft.nbt.ListNBT;

/**
 * Internal interface used in blocks and items that are capable of storing sustained inventories.
 *
 * @author AidanBrady
 */
public interface ISustainedInventory {

    /**
     * Sets the inventory tag list to a new value.
     *
     * @param nbtTags - NBTTagList value to set
     * @param data    - ItemStack parameter if using on item
     */
    void setInventory(ListNBT nbtTags, Object... data);

    /**
     * Gets the inventory tag list from an item or block.
     *
     * @param data - ItemStack parameter if using on item
     *
     * @return inventory tag list
     */
    ListNBT getInventory(Object... data);

    /**
     * Gets if there is an inventory from an item or block.
     *
     * @param data - ItemStack parameter if using on item
     *
     * @return true if there is a non-empty inventory stored, false otherwise
     */
    default boolean hasInventory(Object... data) {
        ListNBT inventory = getInventory(data);
        return inventory != null && !inventory.isEmpty();
    }
}