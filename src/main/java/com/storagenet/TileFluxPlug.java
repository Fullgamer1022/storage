package com.storagenet;

import cofh.api.energy.IEnergyProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/** Flux Plug: encosta num gerador/bateria e puxa a energia para a rede. */
public class TileFluxPlug extends TileFlux {

    private int run(int max, boolean simulate) {
        int total = 0;
        for (int i = 0; i < 6 && total < max; i++) {
            TileEntity te = worldObj.getTileEntity(xCoord + StorageNetwork.DX[i], yCoord + StorageNetwork.DY[i],
                    zCoord + StorageNetwork.DZ[i]);
            if (te instanceof TileFlux || !(te instanceof IEnergyProvider)) continue;
            total += ((IEnergyProvider) te).extractEnergy(ForgeDirection.getOrientation(i ^ 1), max - total, simulate);
        }
        return total;
    }

    @Override
    public int available() {
        return run(LIMIT, true);
    }

    @Override
    public int take(int n) {
        return run(Math.min(n, LIMIT), false);
    }

    @Override
    public int demand() {
        return 0;
    }

    @Override
    public int give(int n) {
        return 0;
    }
}
