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

@Mod(modid = StorageNet.MODID, name = "Storage Net", version = "1.0.0")
public class StorageNet {
    public static final String MODID = "storagenet";

    @Instance(MODID)
    public static StorageNet instance;

    public static SimpleNetworkWrapper NET;
    public static Block cable;
    public static Block drive;
    public static Block grid;
    public static Item disk;

    public static final CreativeTabs tab = new CreativeTabs("storagenet") {
        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(grid);
        }
    };

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cable = new BlockCable();
        drive = new BlockDrive();
        grid = new BlockGrid();
        disk = new ItemStorageDisk();

        GameRegistry.registerBlock(cable, "cable");
        GameRegistry.registerBlock(drive, "drive");
        GameRegistry.registerBlock(grid, "grid");
        GameRegistry.registerItem(disk, "storage_disk");

        GameRegistry.registerTileEntity(TileCable.class, MODID + ":cable");
        GameRegistry.registerTileEntity(TileDrive.class, MODID + ":drive");
        GameRegistry.registerTileEntity(TileGrid.class, MODID + ":grid");

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
        // Disk Drive
        GameRegistry.addRecipe(new ItemStack(drive), "III", "RCR", "III",
                'I', Items.iron_ingot, 'R', Items.redstone, 'C', cable);
        // Grid
        GameRegistry.addRecipe(new ItemStack(grid), "GGG", "ICI", "RRR",
                'G', Blocks.glass_pane, 'I', Items.iron_ingot, 'C', cable, 'R', Items.redstone);
        // Discos (cada um e feito do zero, para nao perder itens ao fazer upgrade)
        GameRegistry.addRecipe(new ItemStack(disk, 1, 0), "IRI", "RPR", "III",
                'I', Items.iron_ingot, 'R', Items.redstone, 'P', Blocks.glass_pane);
        GameRegistry.addRecipe(new ItemStack(disk, 1, 1), "GRG", "RPR", "GRG",
                'G', Items.gold_ingot, 'R', Items.redstone, 'P', Blocks.glass_pane);
        GameRegistry.addRecipe(new ItemStack(disk, 1, 2), "DRD", "RPR", "DRD",
                'D', Items.diamond, 'R', Items.redstone, 'P', Blocks.glass_pane);
        GameRegistry.addRecipe(new ItemStack(disk, 1, 3), "ERE", "RPR", "ERE",
                'E', Items.emerald, 'R', Blocks.redstone_block, 'P', Blocks.glass_pane);
    }
}
