package com.storagenet;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

/** Disk Drive: guarda ate 8 discos e faz a ponte entre a rede e os discos. */
public class TileDrive extends TileEntity implements IInventory, INetworkNode {
    public static final int SLOTS = 8;
    private ItemStack[] disks = new ItemStack[SLOTS];

    public ItemStack insert(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack d = disks[i];
            if (d == null || !(d.getItem() instanceof ItemStorageDisk)) continue;
            int n = DiskData.insert(d, rest);
            if (n > 0) {
                rest.stackSize -= n;
                markDirty();
            }
            if (rest.stackSize <= 0) return null;
        }
        return rest;
    }

    public int extract(ItemStack proto, int amount) {
        int total = 0;
        for (int i = 0; i < SLOTS && total < amount; i++) {
            ItemStack d = disks[i];
            if (d == null || !(d.getItem() instanceof ItemStorageDisk)) continue;
            int n = DiskData.extract(d, proto, amount - total);
            if (n > 0) {
                total += n;
                markDirty();
            }
        }
        return total;
    }

    public void collect(java.util.Map<String, StackEntry> out) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack d = disks[i];
            if (d != null && d.getItem() instanceof ItemStorageDisk) DiskData.collect(d, out);
        }
    }

    public void dropContents() {
        if (worldObj == null || worldObj.isRemote) return;
        for (int i = 0; i < SLOTS; i++) {
            if (disks[i] != null) {
                worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, disks[i]));
                disks[i] = null;
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        disks = new ItemStack[SLOTS];
        NBTTagList list = nbt.getTagList("Disks", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound c = list.getCompoundTagAt(i);
            int slot = c.getByte("Slot");
            if (slot >= 0 && slot < SLOTS) disks[slot] = ItemStack.loadItemStackFromNBT(c);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < SLOTS; i++) {
            if (disks[i] != null) {
                NBTTagCompound c = new NBTTagCompound();
                c.setByte("Slot", (byte) i);
                disks[i].writeToNBT(c);
                list.appendTag(c);
            }
        }
        nbt.setTag("Disks", list);
    }

    // ---- IInventory ----
    @Override
    public int getSizeInventory() {
        return SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return disks[i];
    }

    @Override
    public ItemStack decrStackSize(int i, int n) {
        if (disks[i] == null) return null;
        ItemStack s;
        if (disks[i].stackSize <= n) {
            s = disks[i];
            disks[i] = null;
        } else {
            s = disks[i].splitStack(n);
            if (disks[i].stackSize == 0) disks[i] = null;
        }
        markDirty();
        return s;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack s) {
        disks[i] = s;
        if (s != null && s.stackSize > 1) s.stackSize = 1;
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "container.storagenet.drive";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer p) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                && p.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack s) {
        return s != null && s.getItem() instanceof ItemStorageDisk;
    }
}
