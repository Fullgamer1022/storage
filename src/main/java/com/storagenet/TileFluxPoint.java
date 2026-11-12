package com.storagenet;

import cofh.api.energy.IEnergyReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/** Flux Point: encosta numa maquina e entrega a ela a energia da rede. */
public class TileFluxPoint extends TileFlux {

    private int run(int max, boolean simulate) {
        int total = 0;
        for (int i = 0; i < 6 && total < max; i++) {
            TileEntity te = worldObj.getTileEntity(xCoord + StorageNetwork.DX[i], yCoord + StorageNetwork.DY[i],
                    zCoord + StorageNetwork.DZ[i]);
            if (te instanceof TileFlux || !(te instanceof IEnergyReceiver)) continue;
            total += ((IEnergyReceiver) te).receiveEnergy(ForgeDirection.getOrientation(i ^ 1), max - total, simulate);
        }
        return total;
    }

    @Override
    public int available() {
        return 0;
    }

    @Override
    public int take(int n) {
        return 0;
    }

    @Override
    public int demand() {
        return run(LIMIT, true);
    }

    @Override
    public int give(int n) {
        return run(Math.min(n, LIMIT), false);
    }
}
