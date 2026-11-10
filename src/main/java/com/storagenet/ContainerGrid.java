package com.storagenet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.world.World;

public class ContainerGrid extends Container {
    public static final int PX = 8;
    public static final int PY = 174;

    private final EntityPlayer player;
    private final InventoryCrafting craftMatrix = new InventoryCrafting(this, 3, 3);
    private final IInventory craftResult = new InventoryCraftResult();
    private final World world;
    private final int x, y, z;

    // lado cliente
    public volatile List<StackEntry> entries = new ArrayList<StackEntry>();
    public volatile int version = 0;
    public volatile int status = 0;
    private List<StackEntry> building;

    // lado servidor
    private List<StackEntry> lastSent;
    private int lastStatus = -1;
    private int tick = 0;
    private boolean forceSend = true;
    private final ConcurrentLinkedQueue<MessageGridClick> clicks = new ConcurrentLinkedQueue<MessageGridClick>();

    public ContainerGrid(EntityPlayer player, World world, int x, int y, int z) {
        this.player = player;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        addSlotToContainer(new SlotCrafting(player, craftMatrix, craftResult, 0, 101, 128));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                addSlotToContainer(new Slot(craftMatrix, c + r * 3, 8 + c * 18, 110 + r * 18));
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlotToContainer(new Slot(player.inventory, c + r * 9 + 9, PX + c * 18, PY + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlotToContainer(new Slot(player.inventory, c, PX + c * 18, PY + 58));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return world.getBlock(x, y, z) == StorageNet.request
                && p.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64.0;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inv) {
        craftResult.setInventorySlotContents(0, CraftingManager.getInstance().findMatchingRecipe(craftMatrix, world));
    }

    @Override
    public void onContainerClosed(EntityPlayer p) {
        super.onContainerClosed(p);
        if (world.isRemote) return;
        for (int i = 0; i < 9; i++) {
            ItemStack st = craftMatrix.getStackInSlotOnClosing(i);
            if (st != null) p.dropPlayerItemWithRandomChoice(st, false);
        }
    }

    // ---------- cliente ----------
    public void receive(MessageGridList m) {
        if (m.first) building = new ArrayList<StackEntry>();
        if (building == null) return;
        building.addAll(m.entries);
        if (m.last) {
            status = m.status;
            entries = building;
            building = null;
            version++;
        }
    }

    // ---------- servidor ----------
    public void queueClick(MessageGridClick c) {
        clicks.add(c);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (world.isRemote || !(player instanceof EntityPlayerMP)) return;

        MessageGridClick c;
        while ((c = clicks.poll()) != null) {
            handleClick(c);
            forceSend = true;
        }

        if (forceSend || ++tick % 10 == 0) {
            StorageNetwork.Net net = StorageNetwork.scan(world, x, y, z);
            List<StackEntry> now = net.valid() ? net.list() : new ArrayList<StackEntry>();
            int st = net.status();
            if (forceSend || st != lastStatus || !sameList(now, lastSent)) {
                send(now, st);
                lastSent = now;
                lastStatus = st;
            }
            forceSend = false;
        }
    }

    private static boolean sameList(List<StackEntry> a, List<StackEntry> b) {
        if (b == null || a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            StackEntry p = a.get(i);
            StackEntry q = b.get(i);
            if (p.count != q.count || !p.same(q)) return false;
        }
        return true;
    }

    private void send(List<StackEntry> list, int st) {
        EntityPlayerMP mp = (EntityPlayerMP) player;
        List<StackEntry> batch = new ArrayList<StackEntry>();
        int bytes = 0;
        boolean first = true;
        for (StackEntry e : list) {
            int s = e.byteSize();
            if (s > 30000) continue;
            if (bytes + s > 20000 && !batch.isEmpty()) {
                StorageNet.NET.sendTo(new MessageGridList(first, false, st, batch), mp);
                first = false;
                batch = new ArrayList<StackEntry>();
                bytes = 0;
            }
            batch.add(e);
            bytes += s;
        }
        StorageNet.NET.sendTo(new MessageGridList(first, true, st, batch), mp);
    }

    private void setCursor(EntityPlayerMP mp, ItemStack s) {
        mp.inventory.setItemStack(s);
        mp.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(-1, -1, s));
    }

    private void handleClick(MessageGridClick c) {
        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP mp = (EntityPlayerMP) player;
        if (c.mode == MessageGridClick.REFRESH) {
            forceSend = true;
            return;
        }
        StorageNetwork.Net net = StorageNetwork.scan(world, x, y, z);
        if (!net.valid()) return;
        ItemStack cursor = mp.inventory.getItemStack();

        switch (c.mode) {
            case MessageGridClick.TAKE_STACK:
            case MessageGridClick.TAKE_HALF:
            case MessageGridClick.TAKE_TO_INV: {
                ItemStack proto = c.buildProto();
                if (proto == null) return;
                if (c.mode != MessageGridClick.TAKE_TO_INV && cursor != null) return;
                int max = proto.getMaxStackSize();
                int amount = c.mode == MessageGridClick.TAKE_HALF ? Math.max(1, (max + 1) / 2) : max;
                ItemStack out = net.extract(proto, amount, null);
                if (out == null) return;
                if (c.mode == MessageGridClick.TAKE_TO_INV) {
                    mp.inventory.addItemStackToInventory(out);
                    if (out.stackSize > 0) {
                        ItemStack rest = net.insert(out, null);
                        if (rest != null) mp.dropPlayerItemWithRandomChoice(rest, false);
                    }
                } else {
                    setCursor(mp, out);
                }
                break;
            }
            case MessageGridClick.INSERT_ALL: {
                if (cursor == null) return;
                ItemStack rest = net.insert(cursor.copy(), null);
                setCursor(mp, rest);
                break;
            }
            case MessageGridClick.INSERT_ONE: {
                if (cursor == null) return;
                ItemStack one = cursor.copy();
                one.stackSize = 1;
                ItemStack rest = net.insert(one, null);
                if (rest == null) {
                    cursor.stackSize--;
                    if (cursor.stackSize <= 0) cursor = null;
                    setCursor(mp, cursor);
                }
                break;
            }
            default:
                break;
        }
    }

    /** Shift+clique: resultado/grade vao para o inventario; inventario vai para a rede. */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        if (world.isRemote) return null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack st = slot.getStack();
        ItemStack orig = st.copy();

        if (index == 0) {
            if (!mergeItemStack(st, 10, 46, true)) return null;
            slot.onSlotChange(st, orig);
        } else if (index < 10) {
            if (!mergeItemStack(st, 10, 46, false)) return null;
        } else {
            StorageNetwork.Net net = StorageNetwork.scan(world, x, y, z);
            if (!net.valid()) return null;
            ItemStack rest = net.insert(st.copy(), null);
            int after = rest == null ? 0 : rest.stackSize;
            if (after == st.stackSize) return null;
            if (after == 0) {
                slot.putStack(null);
            } else {
                st.stackSize = after;
                slot.onSlotChanged();
            }
            forceSend = true;
            return orig;
        }
        if (st.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        if (st.stackSize == orig.stackSize) return null;
        slot.onPickupFromSlot(p, st);
        return orig;
    }
}
