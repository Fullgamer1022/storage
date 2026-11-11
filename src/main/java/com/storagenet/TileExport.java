package com.storagenet;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

/**
 * Export Cable: tira da rede os itens do filtro (ate 18) e coloca nos inventarios encostados.
 * Igual ao original: a cada 30/(Speed+1) ticks move ate 2^(Stack+2) itens por filtro.
 * Com o upgrade Stock, mantem no inventario a quantidade definida no filtro.
 */
public class TileExport extends TileFilter {
    @Override
    public boolean isUpgradeable() {
        return true;
    }

    /** Ferramentas, armaduras, espadas, arcos e enxadas ignoram o filtro de dano (igual ao original). */
    private boolean isTool(ItemStack s) {
        Item it = s.getItem();
        return it instanceof ItemArmor || it instanceof ItemTool || it instanceof ItemSword
                || it instanceof ItemBow || it instanceof ItemHoe;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        int period = Math.max(1, 30 / (upgradeCount(ItemUpgrade.SPEED) + 1));
        if ((worldObj.getTotalWorldTime() + 20) % period != 0) return;
        boolean any = false;
        for (ItemStack f : filter) {
            if (f != null) {
                any = true;
                break;
            }
        }
        if (!any) return;
        StorageNetwork.Net net = StorageNetwork.scan(worldObj, xCoord, yCoord, zCoord);
        if (!net.valid() || !status(net)) return;
        int perOp = 1 << (upgradeCount(ItemUpgrade.STACK) + 2);
        boolean stock = upgradeCount(ItemUpgrade.STOCK) > 0;
        for (int i = 0; i < 6; i++) {
            IInventory inv = StorageNetwork.invAt(worldObj,
                    xCoord + StorageNetwork.DX[i], yCoord + StorageNetwork.DY[i], zCoord + StorageNetwork.DZ[i]);
            if (inv == null) continue;
            InvRef ref = new InvRef(inv, i ^ 1, null);
            for (int k = 0; k < SLOTS; k++) {
                ItemStack f = filter[k];
                if (f == null) continue;
                                final boolean useOre = ore[k];
                final boolean useMeta = meta[k] && !isTool(f);
                final ItemStack proto = f;
                StorageNetwork.Matcher m = new StorageNetwork.Matcher() {
                    @Override
                    public boolean matches(ItemStack s) {
                        if (useOre) return oreEqual(s, proto);
                        if (useMeta) return s.getItem() == proto.getItem() && s.getItemDamage() == proto.getItemDamage();
                        return s.getItem() == proto.getItem();
                    }
                };
                ItemStack sample = net.find(m, inv);
                if (sample == null) continue;
                int amount = sample.getMaxStackSize();
                if (stock) {
                    amount = Math.min(amount, Math.max(1, f.stackSize) - ref.count(sample));
                }
                if (amount <= 0) continue;
                amount = Math.min(amount, perOp);
                ItemStack probe = sample.copy();
                probe.stackSize = amount;
                int fits = ref.simulateInsert(probe);
                if (fits <= 0) continue;
                ItemStack pulled = net.extract(sample, fits, inv);
                if (pulled == null) continue;
                ItemStack rest = ref.insert(pulled);
                if (rest != null) {
                    ItemStack back = net.insert(rest, null);
                    if (back != null) {
                        worldObj.spawnEntityInWorld(
                                new EntityItem(worldObj, xCoord + 0.5, yCoord + 1.0, zCoord + 0.5, back));
                    }
                }
            }
        }
    }
}
