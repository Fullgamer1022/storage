package com.storagenet;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Base dos cabos com configuracao (Link, Import e Export): 18 filtros, lista branca/negra,
 * prioridade, direcao (so o Link), 4 upgrades e a "operacao" (item monitorado + limite).
 * Tudo e sincronizado com o cliente pelo pacote de descricao do TileEntity.
 */
public abstract class TileFilter extends TileEntity implements INetworkNode {
    public static final int SLOTS = 18;
    public static final int UPGRADE_SLOTS = 4;

    public enum Way {
        IN, OUT, BOTH;

        public boolean match(Way o) {
            return this == BOTH || o == BOTH || this == o;
        }

        public Way next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** O tamanho do stack do filtro e a quantidade desejada (upgrade Stock). */
    public final ItemStack[] filter = new ItemStack[SLOTS];
    public final boolean[] ore = new boolean[SLOTS];
    public final boolean[] meta = new boolean[SLOTS];
    public final ItemStack[] upgrades = new ItemStack[UPGRADE_SLOTS];
    public boolean white = false;
    public int priority = 0;
    public Way way = Way.BOTH;

    // operacao (upgrade Operation)
    public ItemStack opStack;
    public boolean mode = false; // true: ativo se tiver MAIS que 'limit'; false: ativo se tiver MENOS ou igual
    public int limit = 0;

    public boolean isStorage() {
        return false;
    }

    public boolean isUpgradeable() {
        return false;
    }

    /** Quantidade do primeiro upgrade desse tipo (0 = Speed, 1 = Operation, 2 = Stack, 3 = Stock). */
    public int upgradeCount(int type) {
        for (ItemStack s : upgrades) {
            if (s != null && s.stackSize > 0 && s.getItemDamage() == type) return s.stackSize;
        }
        return 0;
    }

    // ---------- filtro ----------
    public static boolean oreEqual(ItemStack a, ItemStack b) {
        int[] x = OreDictionary.getOreIDs(a);
        int[] y = OreDictionary.getOreIDs(b);
        for (int i : x) {
            for (int j : y) {
                if (i == j) return true;
            }
        }
        return false;
    }

    public boolean matchesSlot(int i, ItemStack s) {
        ItemStack f = filter[i];
        if (f == null) return false;
        if (ore[i]) return oreEqual(s, f);
        if (meta[i]) return s.getItem() == f.getItem() && s.getItemDamage() == f.getItemDamage();
        return s.getItem() == f.getItem();
    }

    public boolean canTransfer(ItemStack s, Way dir) {
        if (isStorage() && !way.match(dir)) return false;
        if (white) {
            for (int i = 0; i < SLOTS; i++) {
                if (filter[i] != null && matchesSlot(i, s)) return true;
            }
            return false;
        }
        for (int i = 0; i < SLOTS; i++) {
            if (filter[i] != null && matchesSlot(i, s)) return false;
        }
        return true;
    }

    /** Upgrade Operation: o cabo so trabalha enquanto a condicao do item monitorado for verdadeira. */
    public boolean status(StorageNetwork.Net net) {
        if (upgradeCount(ItemUpgrade.OP) < 1) return true;
        if (opStack == null) return true;
        int have = net.amount(opStack);
        return mode ? have > limit : have <= limit;
    }

    // ---------- sincronizacao ----------
    public void markUpdate() {
        markDirty();
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound t = new NBTTagCompound();
        writeToNBT(t);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, t);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        for (int i = 0; i < SLOTS; i++) {
            filter[i] = null;
            ore[i] = false;
            meta[i] = false;
        }
        for (int i = 0; i < UPGRADE_SLOTS; i++) upgrades[i] = null;
        NBTTagList fl = nbt.getTagList("Filters", 10);
        for (int k = 0; k < fl.tagCount(); k++) {
            NBTTagCompound c = fl.getCompoundTagAt(k);
            int i = c.getByte("Slot");
            if (i < 0 || i >= SLOTS) continue;
            ItemStack s = ItemStack.loadItemStackFromNBT(c);
            filter[i] = s;
            ore[i] = c.getBoolean("Ore");
            meta[i] = c.getBoolean("Meta");
        }
        NBTTagList ul = nbt.getTagList("Upgrades", 10);
        for (int k = 0; k < ul.tagCount(); k++) {
            NBTTagCompound c = ul.getCompoundTagAt(k);
            int i = c.getByte("Slot");
            if (i < 0 || i >= UPGRADE_SLOTS) continue;
            upgrades[i] = ItemStack.loadItemStackFromNBT(c);
        }
        white = nbt.getBoolean("White");
        priority = nbt.getInteger("Prio");
        way = Way.values()[Math.max(0, Math.min(Way.values().length - 1, nbt.hasKey("Way") ? nbt.getInteger("Way") : Way.BOTH.ordinal()))];
        mode = nbt.getBoolean("Mode");
        limit = nbt.getInteger("Limit");
        opStack = nbt.hasKey("Op") ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("Op")) : null;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        NBTTagList fl = new NBTTagList();
        for (int i = 0; i < SLOTS; i++) {
            if (filter[i] == null) continue;
            NBTTagCompound c = new NBTTagCompound();
            filter[i].writeToNBT(c);
            c.setByte("Slot", (byte) i);
            c.setBoolean("Ore", ore[i]);
            c.setBoolean("Meta", meta[i]);
            fl.appendTag(c);
        }
        nbt.setTag("Filters", fl);
        NBTTagList ul = new NBTTagList();
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            if (upgrades[i] == null) continue;
            NBTTagCompound c = new NBTTagCompound();
            upgrades[i].writeToNBT(c);
            c.setByte("Slot", (byte) i);
            ul.appendTag(c);
        }
        nbt.setTag("Upgrades", ul);
        nbt.setBoolean("White", white);
        nbt.setInteger("Prio", priority);
        nbt.setInteger("Way", way.ordinal());
        nbt.setBoolean("Mode", mode);
        nbt.setInteger("Limit", limit);
        if (opStack != null) nbt.setTag("Op", opStack.writeToNBT(new NBTTagCompound()));
    }

    /** Inventarios encostados neste cabo (nao conta blocos da propria rede). */
    public IInventory[] neighbours() {
        IInventory[] out = new IInventory[6];
        for (int i = 0; i < 6; i++) {
            out[i] = StorageNetwork.invAt(worldObj, xCoord + StorageNetwork.DX[i], yCoord + StorageNetwork.DY[i],
                    zCoord + StorageNetwork.DZ[i]);
        }
        return out;
    }
}
