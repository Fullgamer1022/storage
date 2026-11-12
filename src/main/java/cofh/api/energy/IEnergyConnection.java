package cofh.api.energy;

import net.minecraftforge.common.util.ForgeDirection;

/** Interface da API de energia RF (CoFH), embutida para o Storage Net conversar com outros mods. */
public interface IEnergyConnection {
    boolean canConnectEnergy(ForgeDirection from);
}
