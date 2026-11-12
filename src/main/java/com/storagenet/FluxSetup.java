package com.storagenet;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Registra tudo da parte de redes Flux. Chame FluxSetup.init() uma vez, depois de criar StorageNet.NET. */
public class FluxSetup {
    public static Item fluxCore;
    public static Item fluxConfigurator;
    public static Block fluxController;

    public static void init() {
        fluxCore = new ItemFluxCore();
        fluxConfigurator = new ItemFluxConfigurator();
        fluxController = new BlockFluxController();

        GameRegistry.registerItem(fluxCore, "flux_core");
        GameRegistry.registerItem(fluxConfigurator, "flux_configurator");
        GameRegistry.registerBlock(fluxController, "flux_controller");
        GameRegistry.registerTileEntity(TileFluxController.class, StorageNet.MODID + ":flux_controller");

        StorageNet.NET.registerMessage(MessageNetwork.Handler.class, MessageNetwork.class, 20, Side.SERVER);

        GameRegistry.addRecipe(new ItemStack(fluxCore), "ORO", "RPR", "ORO", 'O', Blocks.obsidian, 'R',
                Items.redstone, 'P', Items.ender_pearl);
        GameRegistry.addRecipe(new ItemStack(fluxConfigurator), "  C", " S ", "S  ", 'C', fluxCore, 'S', Items.stick);
        GameRegistry.addRecipe(new ItemStack(fluxController), "OCO", "ORO", "OOO", 'O', Blocks.obsidian, 'C',
                fluxCore, 'R', Blocks.redstone_block);
    }
}
