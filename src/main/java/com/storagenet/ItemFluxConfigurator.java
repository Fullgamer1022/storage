package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 * Flux Configurator. Clique no ar: abre a tela para criar/entrar numa rede (nome + senha).
 * Agachado + clique num bloco Flux (ou clique normal no Controller): liga o bloco a rede.
 */
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

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            player.openGui(StorageNet.instance, GuiHandler.NETWORK, world, (int) player.posX, (int) player.posY,
                    (int) player.posZ);
        }
        return stack;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hx, float hy, float hz) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileFlux)) return false;
        if (world.isRemote) return true;
        int id = netId(stack);
        if (id <= 0) {
            FluxNetworks.say((net.minecraft.entity.player.EntityPlayerMP) player, "message.storagenet.net_none");
            return true;
        }
        FluxNetwork n = FluxNetworks.get().byId(id);
        if (n == null) {
            FluxNetworks.say((net.minecraft.entity.player.EntityPlayerMP) player, "message.storagenet.net_missing");
            return true;
        }
        TileFlux t = (TileFlux) te;
        t.network = id;
        t.markDirty();
        world.markBlockForUpdate(x, y, z);
        FluxNetworks.say((net.minecraft.entity.player.EntityPlayerMP) player, "message.storagenet.net_applied", n.name);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        String n = netName(stack);
        if (n.length() == 0) n = StatCollector.translateToLocal("gui.storagenet.net.none");
        list.add(StatCollector.translateToLocalFormatted("gui.storagenet.net.current", n));
        list.add(StatCollector.translateToLocal("tooltip.storagenet.flux_configurator"));
    }
}
