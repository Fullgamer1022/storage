package com.storagenet;

import net.minecraft.item.Item;

/** Flux Core: ingrediente das receitas do Flux Configurator e do Flux Controller. */
public class ItemFluxCore extends Item {
    public ItemFluxCore() {
        setUnlocalizedName("storagenet.flux_core");
        setTextureName("storagenet:flux_core");
        setCreativeTab(StorageNet.tab);
    }
}
