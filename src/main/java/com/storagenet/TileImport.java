package com.storagenet;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/** Import Cable: puxa itens dos inventarios encostados e manda para a rede. */
public class TileImport extends TileEntity implements INetworkNode {
    private static final int PER_OP = 16;
    private int timer;

    @Override
    public void updateEntity() {
        if (worldObj.isRemote || ++timer % 10 != 0) return;
        StorageNetwork.Net net = StorageNetwork.scan(worldObj, xCoord, yCoord, zCoord);
        if (!net.valid()) return;
        for (int i = 0; i < 6; i++) {
            IInventory inv = StorageNetwork.invAt(worldObj,
                    xCoord + StorageNetwork.DX[i], yCoord + StorageNetwork.DY[i], zCoord + StorageNetwork.DZ[i]);
            if (inv == null) continue;
            InvRef ref = new InvRef(inv, i ^ 1);
            for (int slot : ref.slots()) {
                if (slot < 0 || slot >= inv.getSizeInventory()) continue;
                ItemStack s = inv.getStackInSlot(slot);
                if (s == null || s.stackSize <= 0 || !ref.canOut(slot, s)) continue;
                ItemStack probe = s.copy();
                probe.stackSize = Math.min(s.stackSize, PER_OP);
                ItemStack rest = net.insert(probe, inv);
                int moved = probe.stackSize - (rest == null ? 0 : rest.stackSize);
                if (moved > 0) {
                    s.stackSize -= moved;
                    if (s.stackSize <= 0) inv.setInventorySlotContents(slot, null);
                    inv.markDirty();
                    break;
                }
            }
        }
    }
}
