package com.storagenet;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
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
import net.minecraftforge.common.config.Configuration;

@Mod(modid = StorageNet.MODID, name = "Storage Net", version = "2.0.0")
public class StorageNet {
    public static final String MODID = "storagenet";

    @Instance(MODID)
    public static StorageNet instance;

    @SidedProxy(clientSide = "com.storagenet.ClientProxy", serverSide = "com.storagenet.CommonProxy")
    public static CommonProxy proxy;

    public static int cableRenderId = -1;

    public static SimpleNetworkWrapper NET;
    public static Block master;
    public static Block cable;
    public static Block link;
    public static Block importCable;
    public static Block exportCable;
    public static Block request;
    public static Item upgrade;
    public static Item remote;
    public static int remoteRange = 128;

    public static final CreativeTabs tab = new CreativeTabs("storagenet") {
        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(master);
        }
    };

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Configuration cfg = new Configuration(e.getSuggestedConfigurationFile());
        remoteRange = cfg.getInt("StorageRemoteRange", "general", 128, 1, 30000000,
                "Alcance (em blocos) do Storage Remote comum. O Advanced nao tem limite.");
        cfg.save();
        proxy.registerRenderers();
        master = new BlockNode("master", TileMaster.class, BlockNode.CUBE);
        cable = new BlockNode("cable", TileCable.class, BlockNode.CABLE);
        link = new BlockNode("link_cable", TileLinkCable.class, BlockNode.CABLE_INV);
        importCable = new BlockNode("import_cable", TileImport.class, BlockNode.CABLE_INV);
        exportCable = new BlockNode("export_cable", TileExport.class, BlockNode.CABLE_INV);
        request = new BlockRequest();

        GameRegistry.registerBlock(master, ItemBlockNode.class, "master");
        GameRegistry.registerBlock(cable, ItemBlockNode.class, "cable");
        GameRegistry.registerBlock(link, ItemBlockNode.class, "link_cable");
        GameRegistry.registerBlock(importCable, ItemBlockNode.class, "import_cable");
        GameRegistry.registerBlock(exportCable, ItemBlockNode.class, "export_cable");
        GameRegistry.registerBlock(request, ItemBlockNode.class, "request");

        upgrade = new ItemUpgrade();
        remote = new ItemRemote();
        GameRegistry.registerItem(upgrade, "upgrade");
        GameRegistry.registerItem(remote, "remote");

        GameRegistry.registerTileEntity(TileMaster.class, MODID + ":master");
        GameRegistry.registerTileEntity(TileCable.class, MODID + ":cable");
        GameRegistry.registerTileEntity(TileLinkCable.class, MODID + ":link_cable");
        GameRegistry.registerTileEntity(TileImport.class, MODID + ":import_cable");
        GameRegistry.registerTileEntity(TileExport.class, MODID + ":export_cable");
        GameRegistry.registerTileEntity(TileRequest.class, MODID + ":request");

        NET = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        NET.registerMessage(MessageGridList.Handler.class, MessageGridList.class, 0, Side.CLIENT);
        NET.registerMessage(MessageGridClick.Handler.class, MessageGridClick.class, 1, Side.SERVER);
        NET.registerMessage(MessageCable.Handler.class, MessageCable.class, 2, Side.SERVER);
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());

        // Receitas iguais as do Simple Storage Network original
        GameRegistry.addRecipe(new ItemStack(cable, 8), "SSS", "II ", "SSS",
                'S', new ItemStack(Blocks.stone_slab, 1, 0), 'I', Items.iron_ingot);
        GameRegistry.addRecipe(new ItemStack(master), "DKD", "KCK", "DKD",
                'K', cable, 'D', new ItemStack(Blocks.quartz_block, 1, 0), 'C', Items.diamond);
        GameRegistry.addRecipe(new ItemStack(request), "DKD", "KCK", "DKD",
                'K', cable, 'D', Items.gold_ingot, 'C', Blocks.crafting_table);
        GameRegistry.addRecipe(new ItemStack(link, 4), " K ", "KPK", " K ", 'K', cable, 'P', Blocks.chest);
        GameRegistry.addRecipe(new ItemStack(importCable, 4), " K ", "KPK", " K ", 'K', cable, 'P', Blocks.hopper);
        GameRegistry.addRecipe(new ItemStack(exportCable, 4), " K ", "KPK", " K ", 'K', cable, 'P', Blocks.piston);
        // Upgrades
        GameRegistry.addRecipe(new ItemStack(upgrade, 4, 0), " C ", "GKG", " C ",
                'C', Blocks.redstone_block, 'K', cable, 'G', Items.gold_ingot);
        GameRegistry.addRecipe(new ItemStack(upgrade, 4, 1), " C ", "GKG", " C ",
                'C', Items.comparator, 'K', cable, 'G', Items.redstone);
        GameRegistry.addRecipe(new ItemStack(upgrade, 4, 2), " C ", "GKG", " C ",
                'C', Blocks.redstone_block, 'K', cable, 'G', Items.blaze_powder);
        GameRegistry.addRecipe(new ItemStack(upgrade, 4, 3), " C ", " K ", " C ",
                'C', Items.comparator, 'K', cable);
        // Controles remotos (o original usa bloco de magma; no 1.7.10 usamos creme de magma)
        GameRegistry.addRecipe(new ItemStack(remote, 1, 0), " C ", "EKE", " C ",
                'C', Items.gold_ingot, 'K', cable, 'E', Items.ender_pearl);
        GameRegistry.addRecipe(new ItemStack(remote, 1, 1), "PCP", "MIM", "PDP",
                'C', Items.blaze_rod, 'I', new ItemStack(remote, 1, 0), 'M', master,
                'D', Items.magma_cream, 'P', Items.ender_pearl);
    }
}
