package cofh.api.energy;

import net.minecraft.item.ItemStack;

/** Stub da API CoFH: itens que guardam energia (RF). */
public interface IEnergyContainerItem {
    int receiveEnergy(ItemStack container, int maxReceive, boolean simulate);

    int extractEnergy(ItemStack container, int maxExtract, boolean simulate);

    int getEnergyStored(ItemStack container);

    int getMaxEnergyStored(ItemStack container);
}
