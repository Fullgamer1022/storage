package com.storagenet;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotDisk extends Slot {
    public SlotDisk(IInventory inv, int index, int x, int y) {
        super(inv, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack s) {
        return s != null && s.getItem() instanceof ItemStorageDisk;
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }
}
