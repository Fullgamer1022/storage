package com.storagenet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/** Container vazio (sem slots) so para a tela de redes ter uma janela no servidor. */
public class ContainerNetwork extends Container {
    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return p.getHeldItem() != null && p.getHeldItem().getItem() instanceof ItemFluxConfigurator;
    }
}
