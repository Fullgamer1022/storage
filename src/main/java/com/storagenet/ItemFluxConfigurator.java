package com.storagenet;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class ItemFluxConfigurator extends Item {
    public ItemFluxConfigurator() {
        setMaxStackSize(1);
        setUnlocalizedName("storagenet.flux_configurator");
        setTextureName("storagenet:flux_configurator");
        setCreativeTab(StorageNet.tab);
    }

    public static int netId(ItemStack s) {
        return s != null && s.hasTagCompound() ? s.getTagCompound().getInteger("Net") : 0;
    }

    public static String netName(ItemStack s) {
        return s != null && s.hasTagCompound() ? s.getTagCompound().getString("NetName") : "";
    }

    /** Clique no ar: abre a GUI de redes. */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            player.openGui(StorageNet.instance, GuiHandler.NETWORK, world, (int) player.posX, (int) player.posY, (int) player.posZ);
        }
        return stack;
    }

    /**
     * Shift + clique num bloco Flux:
     *  - Configurator sem rede, ou bloco ja na rede do Configurator -> abre a GUI do bloco (prioridade, limite, nome)
     *  - caso contrario liga o bloco na rede do Configurator.
     */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hx, float hy, float hz) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileFlux)) return false;
        if (world.isRemote) return true;
        TileFlux t = (TileFlux) te;
        int id = netId(stack);
        EntityPlayerMP p = (EntityPlayerMP) player;
        if (id <= 0 || t.network == id) {
            player.openGui(StorageNet.instance, GuiHandler.FLUX_TILE, world, x, y, z);
            return true;
        }
        FluxNetwork n = FluxNetworks.get().byId(id);
        if (n == null) {
            FluxNetworks.say(p, "message.storagenet.net_missing");
            return true;
        }
        if (n.level(p.getUniqueID().toString()) < 0) {
            FluxNetworks.say(p, "message.storagenet.net_no_access");
            return true;
        }
        t.network = n.id;
        t.markDirty();
        world.markBlockForUpdate(x, y, z);
        FluxNetworks.say(p, "message.storagenet.net_applied", n.name);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean adv) {
        String n = netName(stack);
        if (n.length() == 0) n = StatCollector.translateToLocal("gui.storagenet.net.none");
        list.add(StatCollector.translateToLocalFormatted("gui.storagenet.net.current", new Object[] { n }));
        list.add(StatCollector.translateToLocal("tooltip.storagenet.flux_configurator"));
    }
}
