package com.storagenet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.world.World;

public class ContainerGrid extends Container {
    public static final int PX = 17;
    public static final int PY = 124;

    private final EntityPlayer player;
    private final World world;
    private final int x, y, z;

    // lado cliente
    public volatile List<StackEntry> entries = new ArrayList<StackEntry>();
    public volatile int version = 0;
    private List<StackEntry> building;

    // lado servidor
    private List<StackEntry> lastSent;
    private int tick = 0;
    private boolean forceSend = true;
    private final ConcurrentLinkedQueue<MessageGridClick> clicks = new ConcurrentLinkedQueue<MessageGridClick>();

    public ContainerGrid(EntityPlayer player, World world, int x, int y, int z) {
        this.player = player;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
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
        return world.getBlock(x, y, z) == StorageNet.grid
                && p.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64.0;
    }

    // ---------- cliente ----------
    public void receive(MessageGridList m) {
        if (m.first) building = new ArrayList<StackEntry>();
        if (building == null) return;
        building.addAll(m.entries);
        if (m.last) {
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
            List<StackEntry> now = StorageNetwork.list(StorageNetwork.findDrives(world, x, y, z));
            if (forceSend || !sameList(now, lastSent)) {
                send(now);
                lastSent = now;
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

    private void send(List<StackEntry> list) {
        EntityPlayerMP mp = (EntityPlayerMP) player;
        List<StackEntry> batch = new ArrayList<StackEntry>();
        int bytes = 0;
        boolean first = true;
        for (StackEntry e : list) {
            int s = e.byteSize();
            if (s > 30000) continue;
            if (bytes + s > 20000 && !batch.isEmpty()) {
                StorageNet.NET.sendTo(new MessageGridList(first, false, batch), mp);
                first = false;
                batch = new ArrayList<StackEntry>();
                bytes = 0;
            }
            batch.add(e);
            bytes += s;
        }
        StorageNet.NET.sendTo(new MessageGridList(first, true, batch), mp);
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
        List<TileDrive> drives = StorageNetwork.findDrives(world, x, y, z);
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
                ItemStack out = StorageNetwork.extract(drives, proto, amount);
                if (out == null) return;
                if (c.mode == MessageGridClick.TAKE_TO_INV) {
                    mp.inventory.addItemStackToInventory(out);
                    if (out.stackSize > 0) {
                        ItemStack rest = StorageNetwork.insert(drives, out);
                        if (rest != null) mp.dropPlayerItemWithRandomChoice(rest, false);
                    }
                } else {
                    setCursor(mp, out);
                }
                break;
            }
            case MessageGridClick.INSERT_ALL: {
                if (cursor == null) return;
                ItemStack rest = StorageNetwork.insert(drives, cursor.copy());
                setCursor(mp, rest);
                break;
            }
            case MessageGridClick.INSERT_ONE: {
                if (cursor == null) return;
                ItemStack one = cursor.copy();
                one.stackSize = 1;
                ItemStack rest = StorageNetwork.insert(drives, one);
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

    /** Shift+clique num slot do inventario: manda o stack para a rede. */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        if (world.isRemote) return null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack st = slot.getStack();
        ItemStack orig = st.copy();
        ItemStack rest = StorageNetwork.insert(StorageNetwork.findDrives(world, x, y, z), st.copy());
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
}
