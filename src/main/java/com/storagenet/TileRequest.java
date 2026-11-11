package com.storagenet;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Request Table: guarda a grade de crafting e a ordenacao escolhida (nome / mod / quantidade, crescente ou nao). */
public class TileRequest extends TileNode {
    public final ItemStack[] matrix = new ItemStack[9];
    public int sort = 0;
    public boolean down = false;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        for (int i = 0; i < 9; i++) matrix[i] = null;
        NBTTagList l = nbt.getTagList("Matrix", 10);
        for (int k = 0; k < l.tagCount(); k++) {
            NBTTagCompound c = l.getCompoundTagAt(k);
            int i = c.getByte("Slot");
            if (i >= 0 && i < 9) matrix[i] = ItemStack.loadItemStackFromNBT(c);
        }
        sort = nbt.getInteger("Sort");
        down = nbt.getBoolean("Down");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        NBTTagList l = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            if (matrix[i] == null) continue;
            NBTTagCompound c = new NBTTagCompound();
            matrix[i].writeToNBT(c);
            c.setByte("Slot", (byte) i);
            l.appendTag(c);
        }
        nbt.setTag("Matrix", l);
        nbt.setInteger("Sort", sort);
        nbt.setBoolean("Down", down);
    }
}
