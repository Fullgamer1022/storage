package com.storagenet;

import java.util.Map;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

/** Um inventario (bau, etc.) ligado a rede, visto por um dos lados. */
public class InvRef {
    public final IInventory inv;
    /** Lado do inventario que encosta no cabo (0=baixo,1=cima,2=norte,3=sul,4=oeste,5=leste). */
    public final int side;

    public InvRef(IInventory inv, int side) {
        this.inv = inv;
        this.side = side;
    }

    public int[] slots() {
        if (inv instanceof ISidedInventory) {
            int[] a = ((ISidedInventory) inv).getAccessibleSlotsFromSide(side);
            return a == null ? new int[0] : a;
        }
        int n = inv.getSizeInventory();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i;
        return a;
    }

    private boolean canIn(int slot, ItemStack s) {
        if (!inv.isItemValidForSlot(slot, s)) return false;
        return !(inv instanceof ISidedInventory) || ((ISidedInventory) inv).canInsertItem(slot, s, side);
    }

    public boolean canOut(int slot, ItemStack s) {
        return !(inv instanceof ISidedInventory) || ((ISidedInventory) inv).canExtractItem(slot, s, side);
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    /** Guarda o que der. Retorna o que sobrou (null se coube tudo). */
    public ItemStack insert(ItemStack in) {
        ItemStack rest = in.copy();
        int[] slots = slots();
        for (int pass = 0; pass < 2; pass++) {
            for (int slot : slots) {
                if (rest.stackSize <= 0) break;
                if (slot < 0 || slot >= inv.getSizeInventory()) continue;
                if (!canIn(slot, rest)) continue;
                ItemStack cur = inv.getStackInSlot(slot);
                int limit = Math.min(inv.getInventoryStackLimit(), rest.getMaxStackSize());
                if (pass == 0) {
                    if (cur == null || !same(cur, rest)) continue;
                    int space = Math.min(limit, cur.getMaxStackSize()) - cur.stackSize;
                    if (space <= 0) continue;
                    int move = Math.min(space, rest.stackSize);
                    cur.stackSize += move;
                    rest.stackSize -= move;
                } else {
                    if (cur != null) continue;
                    int move = Math.min(limit, rest.stackSize);
                    if (move <= 0) continue;
                    ItemStack put = rest.copy();
                    put.stackSize = move;
                    inv.setInventorySlotContents(slot, put);
                    rest.stackSize -= move;
                }
            }
        }
        inv.markDirty();
        return rest.stackSize <= 0 ? null : rest;
    }

    /** Tira ate 'amount' itens iguais a 'proto'. Retorna quantos sairam. */
    public int extract(ItemStack proto, int amount) {
        int total = 0;
        for (int slot : slots()) {
            if (total >= amount) break;
            if (slot < 0 || slot >= inv.getSizeInventory()) continue;
            ItemStack cur = inv.getStackInSlot(slot);
            if (cur == null || cur.stackSize <= 0 || !same(cur, proto) || !canOut(slot, cur)) continue;
            int take = Math.min(cur.stackSize, amount - total);
            cur.stackSize -= take;
            if (cur.stackSize <= 0) inv.setInventorySlotContents(slot, null);
            total += take;
        }
        if (total > 0) inv.markDirty();
        return total;
    }

    public void collect(Map<String, StackEntry> out) {
        for (int slot : slots()) {
            if (slot < 0 || slot >= inv.getSizeInventory()) continue;
            ItemStack s = inv.getStackInSlot(slot);
            if (s == null || s.stackSize <= 0 || !canOut(slot, s)) continue;
            StackEntry.add(out, StackEntry.of(s, s.stackSize));
        }
    }
}
