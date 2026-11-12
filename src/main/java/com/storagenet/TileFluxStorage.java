package com.storagenet;

import net.minecraft.nbt.NBTTagCompound;

/** Flux Storage: guarda a energia que sobra na rede e devolve quando falta. */
public class TileFluxStorage extends TileFlux {
    public static final int CAPACITY = 20000000;
    public int stored = 0;

    @Override
    public int available() {
        return Math.min(stored, LIMIT);
    }

    @Override
    public int take(int n) {
        int t = Math.min(Math.min(n, stored), LIMIT);
        if (t > 0) {
            stored -= t;
            markDirty();
        }
        return t;
    }

    @Override
    public int demand() {
        return Math.min(CAPACITY - stored, LIMIT);
    }

    @Override
    public int give(int n) {
        int t = Math.min(Math.min(n, CAPACITY - stored), LIMIT);
        if (t > 0) {
            stored += t;
            markDirty();
        }
        return t;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        stored = Math.max(0, Math.min(CAPACITY, nbt.getInteger("Stored")));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("Stored", stored);
    }
}
