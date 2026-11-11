package com.storagenet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Tela de configuracao dos cabos: 4 slots de upgrade (reais) + 18 filtros (fantasmas, tratados por mensagens). */
public class ContainerCable extends Container {
    public static final int UP_X = 98;
    public static final int UP_Y = 6;

    public final TileFilter tile;
    private final EntityPlayer player;
    private final int upgradeSlots;
    private final ConcurrentLinkedQueue<MessageCable> queue = new ConcurrentLinkedQueue<MessageCable>();

    /** Os upgrades ficam guardados no proprio TileEntity. */
    private class UpgradeInv implements IInventory {
        @Override
        public int getSizeInventory() {
            return TileFilter.UPGRADE_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int i) {
            return tile.upgrades[i];
        }

        @Override
        public ItemStack decrStackSize(int i, int n) {
            ItemStack s = tile.upgrades[i];
            if (s == null) return null;
            ItemStack out;
            if (s.stackSize <= n) {
                out = s;
                tile.upgrades[i] = null;
            } else {
                out = s.splitStack(n);
                if (s.stackSize <= 0) tile.upgrades[i] = null;
            }
            markDirty();
            return out;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int i) {
            return null;
        }

        @Override
        public void setInventorySlotContents(int i, ItemStack s) {
            tile.upgrades[i] = s;
            if (s != null && s.stackSize > getInventoryStackLimit()) s.stackSize = getInventoryStackLimit();
            markDirty();
        }

        @Override
        public String getInventoryName() {
            return "upgrades";
        }

        @Override
        public boolean hasCustomInventoryName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 4;
        }

        @Override
        public void markDirty() {
            if (tile.getWorldObj() != null && !tile.getWorldObj().isRemote) tile.markUpdate();
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer p) {
            return true;
        }

        @Override
        public void openInventory() {
        }

        @Override
        public void closeInventory() {
        }

        @Override
        public boolean isItemValidForSlot(int i, ItemStack s) {
            return true;
        }
    }

    private class UpgradeSlot extends Slot {
        public UpgradeSlot(IInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack s) {
            if (s == null || !(s.getItem() instanceof ItemUpgrade)) return false;
            for (int i = 0; i < TileFilter.UPGRADE_SLOTS; i++) {
                if (i == getSlotIndex()) continue;
                ItemStack o = tile.upgrades[i];
                if (o != null && o.getItemDamage() == s.getItemDamage()) return false;
            }
            return true;
        }

        @Override
        public int getSlotStackLimit() {
            return 4;
        }
    }

    public ContainerCable(EntityPlayer player, TileFilter tile) {
        this.player = player;
        this.tile = tile;
        IInventory inv = new UpgradeInv();
        if (tile.isUpgradeable()) {
            for (int i = 0; i < TileFilter.UPGRADE_SLOTS; i++) {
                addSlotToContainer(new UpgradeSlot(inv, i, UP_X + i * 18, UP_Y));
            }
            upgradeSlots = TileFilter.UPGRADE_SLOTS;
        } else {
            upgradeSlots = 0;
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlotToContainer(new Slot(player.inventory, c + r * 9 + 9, 8 + c * 18, 89 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlotToContainer(new Slot(player.inventory, c, 8 + c * 18, 147));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        if (tile.getWorldObj() == null || tile.getWorldObj().getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile) {
            return false;
        }
        return p.getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack st = slot.getStack();
        ItemStack orig = st.copy();
        int total = inventorySlots.size();
        if (index < upgradeSlots) {
            if (!mergeItemStack(st, upgradeSlots, total, true)) return null;
        } else {
            if (upgradeSlots == 0 || !(st.getItem() instanceof ItemUpgrade)) return null;
            // um upgrade de cada tipo: tenta o primeiro slot livre que aceita
            boolean moved = false;
            for (int i = 0; i < upgradeSlots && !moved; i++) {
                Slot t = (Slot) inventorySlots.get(i);
                if (t.getHasStack() || !t.isItemValid(st)) continue;
                ItemStack put = st.copy();
                put.stackSize = Math.min(st.stackSize, t.getSlotStackLimit());
                t.putStack(put);
                st.stackSize -= put.stackSize;
                moved = true;
            }
            if (!moved) return null;
        }
        if (st.stackSize <= 0) slot.putStack(null);
        else slot.onSlotChanged();
        if (st.stackSize == orig.stackSize) return null;
        slot.onPickupFromSlot(p, st);
        return orig;
    }

    // ---------- servidor: mensagens da tela ----------
    public void queue(MessageCable m) {
        queue.add(m);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (player.worldObj.isRemote) return;
        MessageCable m;
        while ((m = queue.poll()) != null) {
            if (m.x != tile.xCoord || m.y != tile.yCoord || m.z != tile.zCoord) continue;
            handle(m);
        }
    }

    private void handle(MessageCable m) {
        switch (m.action) {
            case MessageCable.PRIORITY:
                if (tile.isStorage()) tile.priority = Math.max(-999, Math.min(999, tile.priority + (m.a >= 0 ? 1 : -1)));
                break;
            case MessageCable.WHITE:
                tile.white = !tile.white;
                break;
            case MessageCable.WAY:
                if (tile.isStorage()) tile.way = tile.way.next();
                break;
            case MessageCable.MODE:
                tile.mode = !tile.mode;
                break;
            case MessageCable.LIMIT:
                tile.limit = Math.max(0, m.a);
                break;
            case MessageCable.IMPORT_FILTER:
                importFilter();
                break;
            case MessageCable.GHOST:
                ghost(m.a, (m.b & 1) != 0, (m.b & 2) != 0);
                break;
            case MessageCable.ADJUST:
                if (m.a >= 0 && m.a < TileFilter.SLOTS && tile.filter[m.a] != null) {
                    int n = tile.filter[m.a].stackSize + m.b;
                    tile.filter[m.a].stackSize = Math.max(1, Math.min(9999, n));
                }
                break;
            case MessageCable.OP_ITEM: {
                ItemStack cur = player.inventory.getItemStack();
                if (cur == null) {
                    tile.opStack = null;
                } else {
                    tile.opStack = cur.copy();
                    tile.opStack.stackSize = 1;
                }
                break;
            }
            default:
                return;
        }
        tile.markUpdate();
    }

    private void ghost(int slot, boolean right, boolean shift) {
        if (slot < 0 || slot >= TileFilter.SLOTS) return;
        ItemStack cur = player.inventory.getItemStack();
        if (cur != null) {
            ItemStack f = cur.copy();
            f.stackSize = Math.max(1, Math.min(9999, cur.stackSize));
            tile.filter[slot] = f;
            tile.ore[slot] = false;
            tile.meta[slot] = false;
        } else if (tile.filter[slot] != null) {
            if (shift) {
                tile.filter[slot] = null;
                tile.ore[slot] = false;
                tile.meta[slot] = false;
            } else if (right) {
                tile.ore[slot] = !tile.ore[slot];
            } else {
                tile.meta[slot] = !tile.meta[slot];
            }
        }
    }

    /** Botao "I": copia os itens do inventario encostado para a lista de filtros. */
    private void importFilter() {
        List<ItemStack> found = new ArrayList<ItemStack>();
        for (IInventory inv : tile.neighbours()) {
            if (inv == null) continue;
            for (int i = 0; i < inv.getSizeInventory() && found.size() < TileFilter.SLOTS; i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s == null || s.stackSize <= 0) continue;
                boolean dup = false;
                for (ItemStack o : found) {
                    if (o.getItem() == s.getItem() && o.getItemDamage() == s.getItemDamage()) {
                        dup = true;
                        break;
                    }
                }
                if (!dup) {
                    ItemStack c = s.copy();
                    c.stackSize = 1;
                    found.add(c);
                }
            }
        }
        if (found.isEmpty()) return;
        for (int i = 0; i < TileFilter.SLOTS; i++) {
            tile.filter[i] = i < found.size() ? found.get(i) : null;
            tile.ore[i] = false;
            tile.meta[i] = false;
        }
    }
}
