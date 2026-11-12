package com.storagenet;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

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
        GameRegistry.registerTileEntity(TileFluxController.class, "storagenet:flux_controller");

        StorageNet.NET.registerMessage(MessageNetwork.Handler.class, MessageNetwork.class, 20, Side.SERVER);
        StorageNet.NET.registerMessage(MessageFluxAction.Handler.class, MessageFluxAction.class, 21, Side.SERVER);
        StorageNet.NET.registerMessage(MessageFluxTile.Handler.class, MessageFluxTile.class, 22, Side.SERVER);
        StorageNet.NET.registerMessage(MessageFluxSync.Handler.class, MessageFluxSync.class, 23, Side.CLIENT);

        GameRegistry.addRecipe(new ItemStack(fluxCore), new Object[] { "ORO", "RPR", "ORO",
                'O', Blocks.obsidian, 'R', Items.redstone, 'P', Items.ender_pearl });
        GameRegistry.addRecipe(new ItemStack(fluxConfigurator), new Object[] { "  C", " S ", "S  ",
                'C', fluxCore, 'S', Items.stick });
        GameRegistry.addRecipe(new ItemStack(fluxController), new Object[] { "OCO", "ORO", "OOO",
                'O', Blocks.obsidian, 'C', fluxCore, 'R', Blocks.redstone_block });
    }
}
