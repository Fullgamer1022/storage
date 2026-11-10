package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

public class ItemStorageDisk extends Item {
    public static final int[] CAPACITY = {1000, 4000, 16000, 64000};
    public static final String[] NAMES = {"1k", "4k", "16k", "64k"};

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public ItemStorageDisk() {
        setHasSubtypes(true);
        setMaxStackSize(1);
        setMaxDamage(0);
        setUnlocalizedName(StorageNet.MODID + ".disk");
        setCreativeTab(StorageNet.tab);
    }

    @Override
    public String getUnlocalizedName(ItemStack s) {
        int m = Math.max(0, Math.min(s.getItemDamage(), NAMES.length - 1));
        return "item." + StorageNet.MODID + ".disk." + NAMES[m];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        icons = new IIcon[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
            icons[i] = reg.registerIcon(StorageNet.MODID + ":disk_" + NAMES[i]);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int dmg) {
        return icons[Math.max(0, Math.min(dmg, icons.length - 1))];
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < NAMES.length; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack s, EntityPlayer p, List list, boolean adv) {
        list.add(DiskData.stored(s) + " / " + DiskData.capacity(s));
    }
}
