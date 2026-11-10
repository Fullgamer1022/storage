package com.storagenet;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Mod(modid = StorageNet.MODID, name = "Storage Net", version = "2.0.0")
public class StorageNet {
    public static final String MODID = "storagenet";

    @Instance(MODID)
    public static StorageNet instance;

    public static SimpleNetworkWrapper NET;
    public static Block master;
    public static Block cable;
    public static Block link;
    public static Block importCable;
    public static Block exportCable;
    public static Block request;

    public static final CreativeTabs tab = new CreativeTabs("storagenet") {
        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(master);
        }
    };

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        master = new BlockNode("master", TileMaster.class);
        cable = new BlockNode("cable", TileCable.class);
        link = new BlockNode("link_cable", TileLinkCable.class);
        importCable = new BlockNode("import_cable", TileImport.class);
        exportCable = new BlockExport();
        request = new BlockRequest();

        GameRegistry.registerBlock(master, "master");
        GameRegistry.registerBlock(cable, "cable");
        GameRegistry.registerBlock(link, "link_cable");
        GameRegistry.registerBlock(importCable, "import_cable");
        GameRegistry.registerBlock(exportCable, "export_cable");
        GameRegistry.registerBlock(request, "request");

        GameRegistry.registerTileEntity(TileMaster.class, MODID + ":master");
        GameRegistry.registerTileEntity(TileCable.class, MODID + ":cable");
        GameRegistry.registerTileEntity(TileLinkCable.class, MODID + ":link_cable");
        GameRegistry.registerTileEntity(TileImport.class, MODID + ":import_cable");
        GameRegistry.registerTileEntity(TileExport.class, MODID + ":export_cable");
        GameRegistry.registerTileEntity(TileRequest.class, MODID + ":request");

        NET = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        NET.registerMessage(MessageGridList.Handler.class, MessageGridList.class, 0, Side.CLIENT);
        NET.registerMessage(MessageGridClick.Handler.class, MessageGridClick.class, 1, Side.SERVER);
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());

        // Cabo (x4)
        GameRegistry.addRecipe(new ItemStack(cable, 4), "IRI",
                'I', Items.iron_ingot, 'R', Items.redstone);
        // Master
        GameRegistry.addRecipe(new ItemStack(master), "IRI", "RDR", "IRI",
                'I', Items.iron_ingot, 'R', Items.redstone, 'D', Items.diamond);
        // Request Table
        GameRegistry.addRecipe(new ItemStack(request), "GGG", "ICI", "RRR",
                'G', Blocks.glass_pane, 'I', Items.iron_ingot, 'C', cable, 'R', Items.redstone);
        // Link / Import / Export: cabo + um bloco
        GameRegistry.addShapelessRecipe(new ItemStack(link), cable, Blocks.chest);
        GameRegistry.addShapelessRecipe(new ItemStack(importCable), cable, Blocks.hopper);
        GameRegistry.addShapelessRecipe(new ItemStack(exportCable), cable, Blocks.dropper);
    }
}
