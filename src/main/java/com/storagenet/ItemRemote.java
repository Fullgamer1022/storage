package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Storage Remote (dano 0: alcance limitado, mesma dimensao) e Advanced Storage Remote (dano 1: sem limite).
 * Clique direito num Master para ligar o controle; clique direito no ar para abrir a rede.
 */
public class ItemRemote extends Item {
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public ItemRemote() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setMaxStackSize(1);
        setCreativeTab(StorageNet.tab);
        setUnlocalizedName(StorageNet.MODID + ".remote");
    }

    private static int type(ItemStack s) {
        return Math.max(0, Math.min(1, s.getItemDamage()));
    }

    @Override
    public String getUnlocalizedName(ItemStack s) {
        return "item." + StorageNet.MODID + ".remote_" + type(s);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        icons = new IIcon[2];
        for (int i = 0; i < 2; i++) icons[i] = reg.registerIcon(StorageNet.MODID + ":remote_" + i);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int dmg) {
        return icons[Math.max(0, Math.min(1, dmg))];
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
    }

    public static boolean isBound(ItemStack s) {
        return s != null && s.hasTagCompound() && s.getTagCompound().getBoolean("bound");
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack s, EntityPlayer p, List list, boolean adv) {
        list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                "tooltip.storagenet.remote_" + type(s), StorageNet.remoteRange));
        if (isBound(s)) {
            NBTTagCompound t = s.getTagCompound();
            list.add(EnumChatFormatting.DARK_AQUA + StatCollector.translateToLocalFormatted("tooltip.storagenet.remote_bound",
                    t.getInteger("dim"), t.getInteger("x"), t.getInteger("y"), t.getInteger("z")));
        }
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer p, World w, int x, int y, int z, int side,
                             float hx, float hy, float hz) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (!(te instanceof TileMaster)) return false;
        if (!w.isRemote) {
            NBTTagCompound t = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
            t.setInteger("x", x);
            t.setInteger("y", y);
            t.setInteger("z", z);
            t.setInteger("dim", w.provider.dimensionId);
            t.setBoolean("bound", true);
            stack.setTagCompound(t);
            p.addChatMessage(new ChatComponentTranslation("message.storagenet.remote_bound"));
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World w, EntityPlayer p) {
        if (w.isRemote) return stack;
        if (!isBound(stack)) {
            p.addChatMessage(new ChatComponentTranslation("message.storagenet.remote_unbound"));
            return stack;
        }
        NBTTagCompound t = stack.getTagCompound();
        int x = t.getInteger("x");
        int y = t.getInteger("y");
        int z = t.getInteger("z");
        WorldServer mw = MinecraftServer.getServer().worldServerForDimension(t.getInteger("dim"));
        if (mw == null || !mw.blockExists(x, y, z) || !(mw.getTileEntity(x, y, z) instanceof TileMaster)) {
            p.addChatMessage(new ChatComponentTranslation("message.storagenet.remote_not_loaded"));
            return stack;
        }
        if (!inRange(stack, p)) {
            p.addChatMessage(new ChatComponentTranslation("message.storagenet.remote_range"));
            return stack;
        }
        p.openGui(StorageNet.instance, GuiHandler.REMOTE, w, 0, 0, 0);
        return stack;
    }

    /** O Advanced Remote funciona de qualquer lugar; o comum so na mesma dimensao e dentro do alcance. */
    public static boolean inRange(ItemStack stack, EntityPlayer p) {
        if (stack == null || !isBound(stack)) return false;
        if (type(stack) == 1) return true;
        NBTTagCompound t = stack.getTagCompound();
        if (t.getInteger("dim") != p.worldObj.provider.dimensionId) return false;
        return p.getDistance(t.getInteger("x") + 0.5, t.getInteger("y") + 0.5, t.getInteger("z") + 0.5)
                <= StorageNet.remoteRange;
    }
}
