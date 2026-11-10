package com.storagenet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerDrive extends Container {
    private final TileDrive tile;

    public ContainerDrive(InventoryPlayer inv, TileDrive tile) {
        this.tile = tile;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 4; c++) {
                addSlotToContainer(new SlotDisk(tile, r * 4 + c, 53 + c * 18, 20 + r * 18));
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlotToContainer(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlotToContainer(new Slot(inv, c, 8 + c * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return tile.isUseableByPlayer(p);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack st = slot.getStack();
        ItemStack orig = st.copy();
        if (index < TileDrive.SLOTS) {
            if (!mergeItemStack(st, TileDrive.SLOTS, TileDrive.SLOTS + 36, true)) return null;
        } else {
            if (!(st.getItem() instanceof ItemStorageDisk)) return null;
            if (!mergeItemStack(st, 0, TileDrive.SLOTS, false)) return null;
        }
        if (st.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return orig;
    }
}
