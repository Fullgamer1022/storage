package com.storagenet;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Export Cable: tira da rede o item do filtro e coloca nos inventarios encostados. */
public class TileExport extends TileEntity implements INetworkNode {
    private static final int PER_OP = 16;
    private int timer;
    private ItemStack filter;

    public ItemStack getFilter() {
        return filter;
    }

    public void setFilter(ItemStack f) {
        filter = f;
        markDirty();
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote || filter == null || ++timer % 10 != 0) return;
        StorageNetwork.Net net = StorageNetwork.scan(worldObj, xCoord, yCoord, zCoord);
        if (!net.valid()) return;
        for (int i = 0; i < 6; i++) {
            IInventory inv = StorageNetwork.invAt(worldObj,
                    xCoord + StorageNetwork.DX[i], yCoord + StorageNetwork.DY[i], zCoord + StorageNetwork.DZ[i]);
            if (inv == null) continue;
            ItemStack proto = filter.copy();
            proto.stackSize = 1;
            ItemStack pulled = net.extract(proto, PER_OP, inv);
            if (pulled == null) continue;
            ItemStack rest = new InvRef(inv, i ^ 1).insert(pulled);
            if (rest != null) {
                ItemStack back = net.insert(rest, null);
                if (back != null) {
                    worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord + 0.5, yCoord + 1.0, zCoord + 0.5, back));
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        filter = nbt.hasKey("Filter") ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("Filter")) : null;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (filter != null) nbt.setTag("Filter", filter.writeToNBT(new NBTTagCompound()));
    }
}
