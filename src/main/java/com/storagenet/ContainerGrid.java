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
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Tela da Request Table e dos controles remotos (Storage Remote). Na mesa, a grade de crafting fica guardada
 * no proprio bloco; no controle, o que sobra volta para a rede / inventario ao fechar.
 */
public class ContainerGrid extends Container {
    public static final int PX = 8;
    public static final int PY = 174;

    private final EntityPlayer player;
    private InventoryCrafting craftMatrix;
    private final IInventory craftResult = new InventoryCraftResult();
    private final World world;
    private final int x, y, z;
    private final boolean remoteMode;

    // lado cliente
    public volatile List<StackEntry> entries = new ArrayList<StackEntry>();
    public volatile int version = 0;
    public volatile int status = 0;
    public volatile int sort = 0;
    public volatile boolean down = false;
    private List<StackEntry> building;

    // lado servidor
    private List<StackEntry> lastSent;
    private int lastStatus = -1;
    private int tick = 0;
    private boolean forceSend = true;
    private final ConcurrentLinkedQueue<MessageGridClick> clicks = new ConcurrentLinkedQueue<MessageGridClick>();

    /** A grade de crafting da mesa: os itens ficam no TileRequest (varios jogadores veem a mesma grade). */
    private class MatrixInv extends InventoryCrafting {
        private final TileRequest t;
        private final Container handler;

        public MatrixInv(Container handler, TileRequest t) {
            super(handler, 3, 3);
            this.handler = handler;
            this.t = t;
        }

        @Override
        public ItemStack getStackInSlot(int i) {
            return i >= 9 ? null : t.matrix[i];
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int i) {
            return null;
        }

        @Override
        public ItemStack decrStackSize(int i, int n) {
            ItemStack s = t.matrix[i];
            if (s == null) return null;
            ItemStack out;
            if (s.stackSize <= n) {
                out = s;
                t.matrix[i] = null;
            } else {
                out = s.splitStack(n);
                if (s.stackSize <= 0) t.matrix[i] = null;
            }
            t.markDirty();
            handler.onCraftMatrixChanged(this);
            return out;
        }

        @Override
        public void setInventorySlotContents(int i, ItemStack s) {
            t.matrix[i] = s;
            t.markDirty();
            handler.onCraftMatrixChanged(this);
        }
    }

    /** Slot de resultado: depois de fabricar, repoe os ingredientes pegando da rede. */
    private class ResultSlot extends SlotCrafting {
        public ResultSlot(EntityPlayer p, IInventory matrix, IInventory result, int idx, int sx, int sy) {
            super(p, matrix, result, idx, sx, sy);
        }

        @Override
        public void onPickupFromSlot(EntityPlayer p, ItemStack stack) {
            ItemStack[] before = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                ItemStack s = craftMatrix.getStackInSlot(i);
                before[i] = s == null ? null : s.copy();
            }
            super.onPickupFromSlot(p, stack);
            if (world.isRemote || !(p instanceof EntityPlayerMP)) return;
            refill(before);
        }
    }

    public ContainerGrid(EntityPlayer player, World world, int x, int y, int z) {
        this.player = player;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.remoteMode = false;
        TileEntity te = world.getTileEntity(x, y, z);
        craftMatrix = te instanceof TileRequest ? new MatrixInv(this, (TileRequest) te) : new InventoryCrafting(this, 3, 3);
        initSlots();
    }

    /** Versao do controle remoto: o Master vem das coordenadas gravadas no item. */
    public ContainerGrid(EntityPlayer player) {
        this.player = player;
        this.remoteMode = true;
        World w = player.worldObj;
        int px = 0, py = 0, pz = 0;
        ItemStack held = player.getHeldItem();
        if (!player.worldObj.isRemote && held != null && held.hasTagCompound()) {
            NBTTagCompound t = held.getTagCompound();
            px = t.getInteger("x");
            py = t.getInteger("y");
            pz = t.getInteger("z");
            WorldServer ws = MinecraftServer.getServer().worldServerForDimension(t.getInteger("dim"));
            if (ws != null) w = ws;
        }
        this.world = w;
        this.x = px;
        this.y = py;
        this.z = pz;
        craftMatrix = new InventoryCrafting(this, 3, 3);
        initSlots();
    }

    private void initSlots() {
        addSlotToContainer(new ResultSlot(player, craftMatrix, craftResult, 0, 101, 128));
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
        onCraftMatrixChanged(craftMatrix);
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        if (remoteMode) {
            if (p.worldObj.isRemote) return true;
            ItemStack h = p.getHeldItem();
            return h != null && h.getItem() instanceof ItemRemote && ItemRemote.inRange(h, p);
        }
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
        if (world.isRemote || !remoteMode) return;
        // controle remoto: devolve a grade para a rede, depois para o inventario
        StorageNetwork.Net net = StorageNetwork.scan(world, x, y, z);
        for (int i = 0; i < 9; i++) {
            ItemStack st = craftMatrix.getStackInSlotOnClosing(i);
            if (st == null) continue;
            if (net.valid()) st = net.insert(st, null);
            if (st != null && !p.inventory.addItemStackToInventory(st)) p.dropPlayerItemWithRandomChoice(st, false);
        }
    }

    // ---------- cliente ----------
    public void receive(MessageGridList m) {
        if (m.first) building = new ArrayList<StackEntry>();
        if (building == null) return;
        building.addAll(m.entries);
        if (m.last) {
            status = m.status;
            sort = m.sort;
            down = m.down;
            entries = building;
            building = null;
            version++;
        }
    }

    // ---------- servidor ----------
    public void queueClick(MessageGridClick c) {
        clicks.add(c);
    }

    private int getSort() {
        if (remoteMode) {
            ItemStack h = player.getHeldItem();
            return h != null && h.hasTagCompound() ? h.getTagCompound().getInteger("sort") : 0;
        }
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof TileRequest ? ((TileRequest) t).sort : 0;
    }

    private boolean getDown() {
        if (remoteMode) {
            ItemStack h = player.getHeldItem();
            return h != null && h.hasTagCompound() && h.getTagCompound().getBoolean("down");
        }
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof TileRequest && ((TileRequest) t).down;
    }

    private void saveSettings(int s, boolean d) {
        if (remoteMode) {
            ItemStack h = player.getHeldItem();
            if (h == null) return;
            NBTTagCompound t = h.hasTagCompound() ? h.getTagCompound() : new NBTTagCompound();
            t.setInteger("sort", s);
            t.setBoolean("down", d);
            h.setTagCompound(t);
        } else {
            TileEntity t = world.getTileEntity(x, y, z);
            if (t instanceof TileRequest) {
                ((TileRequest) t).sort = s;
                ((TileRequest) t).down = d;
                t.markDirty();
            }
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (player.worldObj.isRemote || !(player instanceof EntityPlayerMP)) return;

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
        int srt = getSort();
        boolean dwn = getDown();
        List<StackEntry> batch = new ArrayList<StackEntry>();
        int bytes = 0;
        boolean first = true;
        for (StackEntry e : list) {
            int s = e.byteSize();
            if (s > 30000) continue;
            if (bytes + s > 20000 && !batch.isEmpty()) {
                StorageNet.NET.sendTo(new MessageGridList(first, false, st, srt, dwn, batch), mp);
                first = false;
                batch = new ArrayList<StackEntry>();
                bytes = 0;
            }
            batch.add(e);
            bytes += s;
        }
        StorageNet.NET.sendTo(new MessageGridList(first, true, st, srt, dwn, batch), mp);
    }

    private void setCursor(EntityPlayerMP mp, ItemStack s) {
        mp.inventory.setItemStack(s);
        mp.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(-1, -1, s));
    }

    /** Depois de fabricar: cada casa da grade que ficou vazia recebe de volta o mesmo item, vindo da rede. */
    private void refill(ItemStack[] before) {
        StorageNetwork.Net net = StorageNetwork.scan(world, x, y, z);
        if (!net.valid()) return;
        for (int i = 0; i < 9; i++) {
            if (before[i] == null || craftMatrix.getStackInSlot(i) != null) continue;
            ItemStack proto = before[i].copy();
            proto.stackSize = 1;
            ItemStack got = net.extract(proto, 1, null);
            if (got != null) craftMatrix.setInventorySlotContents(i, got);
        }
        forceSend = true;
    }

    /** Botao do quadradinho: manda tudo que esta na grade para a rede (o que sobrar vai para o inventario). */
    private void clearGrid(EntityPlayerMP mp) {
        StorageNetwork.Net net = StorageNetwork.scan(world, x, y, z);
        for (int i = 0; i < 9; i++) {
            ItemStack st = craftMatrix.getStackInSlot(i);
            if (st == null) continue;
            craftMatrix.setInventorySlotContents(i, null);
            if (net.valid()) st = net.insert(st, null);
            if (st != null && !mp.inventory.addItemStackToInventory(st)) mp.dropPlayerItemWithRandomChoice(st, false);
        }
        mp.inventoryContainer.detectAndSendChanges();
    }

    private void handleClick(MessageGridClick c) {
        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP mp = (EntityPlayerMP) player;
        if (c.mode == MessageGridClick.REFRESH) {
            forceSend = true;
            return;
        }
        if (c.mode == MessageGridClick.SORT_NEXT) {
            saveSettings((getSort() + 1) % 3, getDown());
            return;
        }
        if (c.mode == MessageGridClick.DIR_TOGGLE) {
            saveSettings(getSort(), !getDown());
            return;
        }
        if (c.mode == MessageGridClick.CLEAR_GRID) {
            clearGrid(mp);
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
