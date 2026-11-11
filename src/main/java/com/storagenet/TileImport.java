package com.storagenet;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Import Cable: puxa itens dos inventarios encostados e manda para a rede.
 * Igual ao original: a cada 30/(Speed+1) ticks move ate 2^(Stack+2) itens de UM tipo por inventario.
 */
public class TileImport extends TileFilter {
    @Override
    public boolean isUpgradeable() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        int period = Math.max(1, 30 / (upgradeCount(ItemUpgrade.SPEED) + 1));
        if ((worldObj.getTotalWorldTime() + 10) % period != 0) return;
        StorageNetwork.Net net = StorageNetwork.scan(worldObj, xCoord, yCoord, zCoord);
        if (!net.valid() || !status(net)) return;
        int perOp = 1 << (upgradeCount(ItemUpgrade.STACK) + 2);
        for (int i = 0; i < 6; i++) {
            IInventory inv = StorageNetwork.invAt(worldObj,
                    xCoord + StorageNetwork.DX[i], yCoord + StorageNetwork.DY[i], zCoord + StorageNetwork.DZ[i]);
            if (inv == null) continue;
            InvRef ref = new InvRef(inv, i ^ 1, null);
            for (int slot : ref.slots()) {
                if (slot < 0 || slot >= inv.getSizeInventory()) continue;
                ItemStack s = inv.getStackInSlot(slot);
                if (s == null || s.stackSize <= 0 || !ref.canOut(slot, s)) continue;
                if (!canTransfer(s, Way.OUT)) continue;
                ItemStack probe = s.copy();
                probe.stackSize = Math.min(s.stackSize, perOp);
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
