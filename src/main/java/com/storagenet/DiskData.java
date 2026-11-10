package com.storagenet;

import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Lê e grava o conteudo de um disco de armazenamento (tudo fica no NBT do proprio item). */
public class DiskData {

    private static NBTTagCompound root(ItemStack disk) {
        if (disk.getTagCompound() == null) disk.setTagCompound(new NBTTagCompound());
        return disk.getTagCompound();
    }

    public static int capacity(ItemStack disk) {
        int m = disk.getItemDamage();
        if (m < 0 || m >= ItemStorageDisk.CAPACITY.length) m = 0;
        return ItemStorageDisk.CAPACITY[m];
    }

    public static int stored(ItemStack disk) {
        return disk.hasTagCompound() ? disk.getTagCompound().getInteger("Total") : 0;
    }

    private static boolean matches(NBTTagCompound e, ItemStack s) {
        if (e.getInteger("id") != Item.getIdFromItem(s.getItem())) return false;
        if (e.getInteger("dmg") != s.getItemDamage()) return false;
        boolean eHas = e.hasKey("tag");
        boolean sHas = s.hasTagCompound();
        if (eHas != sHas) return false;
        return !eHas || e.getCompoundTag("tag").equals(s.getTagCompound());
    }

    /** Guarda o maximo possivel no disco. Retorna quantos itens foram aceitos. */
    public static int insert(ItemStack disk, ItemStack in) {
        int free = capacity(disk) - stored(disk);
        if (free <= 0) return 0;
        int amount = Math.min(free, in.stackSize);
        if (amount <= 0) return 0;
        NBTTagCompound root = root(disk);
        NBTTagList list = root.getTagList("Items", 10);
        boolean found = false;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            if (matches(e, in)) {
                e.setInteger("cnt", e.getInteger("cnt") + amount);
                found = true;
                break;
            }
        }
        if (!found) {
            NBTTagCompound e = new NBTTagCompound();
            e.setInteger("id", Item.getIdFromItem(in.getItem()));
            e.setInteger("dmg", in.getItemDamage());
            e.setInteger("cnt", amount);
            if (in.hasTagCompound()) e.setTag("tag", in.getTagCompound().copy());
            list.appendTag(e);
        }
        root.setTag("Items", list);
        root.setInteger("Total", stored(disk) + amount);
        return amount;
    }

    /** Tira ate 'amount' itens iguais a 'proto' do disco. Retorna quantos saíram. */
    public static int extract(ItemStack disk, ItemStack proto, int amount) {
        if (!disk.hasTagCompound() || amount <= 0) return 0;
        NBTTagCompound root = disk.getTagCompound();
        NBTTagList list = root.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            if (!matches(e, proto)) continue;
            int cnt = e.getInteger("cnt");
            int take = Math.min(cnt, amount);
            if (take >= cnt) {
                list.removeTag(i);
            } else {
                e.setInteger("cnt", cnt - take);
            }
            root.setTag("Items", list);
            root.setInteger("Total", Math.max(0, stored(disk) - take));
            return take;
        }
        return 0;
    }

    public static void collect(ItemStack disk, Map<String, StackEntry> out) {
        if (!disk.hasTagCompound()) return;
        NBTTagList list = disk.getTagCompound().getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            int cnt = e.getInteger("cnt");
            if (cnt <= 0) continue;
            NBTTagCompound tag = e.hasKey("tag") ? (NBTTagCompound) e.getCompoundTag("tag").copy() : null;
            StackEntry se = new StackEntry(e.getInteger("id"), e.getInteger("dmg"), cnt, tag);
            String key = se.id + ":" + se.dmg + ":" + (tag == null ? 0 : tag.hashCode());
            StackEntry ex = out.get(key);
            if (ex != null && ex.same(se)) {
                ex.count += cnt;
            } else {
                out.put(key, se);
            }
        }
    }
}
