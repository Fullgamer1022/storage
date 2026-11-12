package com.storagenet;

import cofh.api.energy.IEnergyContainerItem;
import java.util.ArrayList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Flux Controller: carrega sem fio os itens de energia (RF) dos jogadores proximos,
 * usando a energia da rede Flux a que esta ligado.
 */
public class TileFluxController extends TileFlux {
    /** Alcance em blocos (mesma dimensao). */
    public static final int RADIUS = 64;

    private int run(int max, boolean simulate) {
        if (worldObj == null) return 0;
        int total = 0;
        double r2 = (double) RADIUS * RADIUS;
        for (Object o : new ArrayList<Object>(worldObj.playerEntities)) {
            if (total >= max) break;
            EntityPlayer p = (EntityPlayer) o;
            if (p.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) > r2) continue;
            total += charge(p.inventory.mainInventory, max - total, simulate);
            total += charge(p.inventory.armorInventory, max - total, simulate);
        }
        return total;
    }

    private static int charge(ItemStack[] stacks, int max, boolean simulate) {
        int total = 0;
        for (ItemStack s : stacks) {
            if (total >= max) break;
            if (s != null && s.getItem() instanceof IEnergyContainerItem) {
                total += ((IEnergyContainerItem) s.getItem()).receiveEnergy(s, max - total, simulate);
            }
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
