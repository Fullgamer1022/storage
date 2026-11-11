package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

/** Upgrades dos cabos Import/Export. Dano do item: 0 Speed, 1 Operation, 2 Stack, 3 Stock. */
public class ItemUpgrade extends Item {
    public static final int SPEED = 0;
    public static final int OP = 1;
    public static final int STACK = 2;
    public static final int STOCK = 3;
    public static final int NUM = 4;

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public ItemUpgrade() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setCreativeTab(StorageNet.tab);
        setUnlocalizedName(StorageNet.MODID + ".upgrade");
    }

    private static int type(ItemStack s) {
        return Math.max(0, Math.min(NUM - 1, s.getItemDamage()));
    }

    @Override
    public String getUnlocalizedName(ItemStack s) {
        return "item." + StorageNet.MODID + ".upgrade_" + type(s);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        icons = new IIcon[NUM];
        for (int i = 0; i < NUM; i++) icons[i] = reg.registerIcon(StorageNet.MODID + ":upgrade_" + i);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int dmg) {
        return icons[Math.max(0, Math.min(NUM - 1, dmg))];
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < NUM; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack s, EntityPlayer p, List list, boolean adv) {
        list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.storagenet.upgrade_" + type(s)));
    }
}
