package com.storagenet;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;

public class FluxNetworks extends WorldSavedData {
    public static final String ID = "storagenet_flux_networks";
    public static final int MAX_NAME = 24;
    public static final Queue<Request> QUEUE = new ConcurrentLinkedQueue<Request>();
    public static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<Runnable>();

    private final Map<Integer, FluxNetwork> nets = new LinkedHashMap<Integer, FluxNetwork>();
    private int nextId = 1;

    public FluxNetworks() {
        super(ID);
    }

    public FluxNetworks(String name) {
        super(name);
    }

    public static FluxNetworks get() {
        WorldServer w = MinecraftServer.getServer().worldServers[0];
        FluxNetworks d = (FluxNetworks) w.mapStorage.loadData(FluxNetworks.class, ID);
        if (d == null) {
            d = new FluxNetworks(ID);
            w.mapStorage.setData(ID, d);
        }
        return d;
    }

    public FluxNetwork byId(int id) {
        return nets.get(Integer.valueOf(id));
    }

    public FluxNetwork byName(String name) {
        for (FluxNetwork n : nets.values()) {
            if (n.name.equalsIgnoreCase(name)) return n;
        }
        return null;
    }

    public Collection<FluxNetwork> all() {
        return nets.values();
    }

    public FluxNetwork create(String name, String pass, String owner) {
        FluxNetwork n = new FluxNetwork(nextId++, name, pass, owner);
        nets.put(Integer.valueOf(n.id), n);
        markDirty();
        return n;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        nets.clear();
        nextId = Math.max(1, tag.getInteger("Next"));
        NBTTagList list = tag.getTagList("Nets", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound t = list.getCompoundTagAt(i);
            int id = t.getInteger("Id");
            if (id <= 0) continue;
            FluxNetwork n = new FluxNetwork(id, t.getString("Name"), t.getString("Pass"), t.getString("Owner"));
            n.ownerName = t.getString("OwnerName");
            if (t.hasKey("Color")) n.color = t.getInteger("Color");
            NBTTagList ml = t.getTagList("Members", 10);
            for (int j = 0; j < ml.tagCount(); j++) {
                NBTTagCompound m = ml.getCompoundTagAt(j);
                n.members.put(m.getString("Id"), Integer.valueOf(m.getInteger("Lv")));
                n.memberNames.put(m.getString("Id"), m.getString("Name"));
            }
            nets.put(Integer.valueOf(id), n);
            if (id >= nextId) nextId = id + 1;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Next", nextId);
        NBTTagList list = new NBTTagList();
        for (FluxNetwork n : nets.values()) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("Id", n.id);
            t.setString("Name", n.name);
            t.setString("Pass", n.password);
            t.setString("Owner", n.owner);
            t.setString("OwnerName", n.ownerName);
            t.setInteger("Color", n.color);
            NBTTagList ml = new NBTTagList();
            for (Map.Entry<String, Integer> e : n.members.entrySet()) {
                NBTTagCompound m = new NBTTagCompound();
                m.setString("Id", e.getKey());
                m.setInteger("Lv", e.getValue().intValue());
                String nm = n.memberNames.get(e.getKey());
                m.setString("Name", nm == null ? "" : nm);
                ml.appendTag(m);
            }
            t.setTag("Members", ml);
            list.appendTag(t);
        }
        tag.setTag("Nets", list);
    }

    // ------------------------------------------------------------------ fila

    public static class Request {
        public final EntityPlayerMP player;
        public final int type;
        public final String s1, s2;
        public final int i1, i2;

        /** compat: criar ou entrar pelo nome (MessageNetwork antigo) */
        public Request(EntityPlayerMP player, String name, String pass) {
            this(player, 0, name, pass, 0, 0);
        }

        public Request(EntityPlayerMP player, int type, String s1, String s2, int i1, int i2) {
            this.player = player;
            this.type = type;
            this.s1 = s1;
            this.s2 = s2;
            this.i1 = i1;
            this.i2 = i2;
        }
    }

    public static void processQueue() {
        Request r;
        while ((r = QUEUE.poll()) != null) {
            try {
                handle(r);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        Runnable run;
        while ((run = TASKS.poll()) != null) {
            try {
                run.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static void addMember(FluxNetwork n, EntityPlayerMP p, int lv) {
        String uid = p.getUniqueID().toString();
        n.members.put(uid, Integer.valueOf(lv));
        n.memberNames.put(uid, p.getCommandSenderName());
    }

    private static boolean validName(String s) {
        return s != null && s.length() > 0 && s.length() <= MAX_NAME;
    }

    private static void handle(Request r) {
        EntityPlayerMP p = r.player;
        if (p == null) return;
        String uid = p.getUniqueID().toString();
        FluxNetworks f = get();
        FluxNetwork n = f.byId(r.i1);
        int lv = n == null ? -1 : n.level(uid);
        String s1 = r.s1 == null ? "" : r.s1;
        String s2 = r.s2 == null ? "" : r.s2;
        switch (r.type) {
            case 0: { // compat: criar/entrar pelo nome
                String nm = s1.trim();
                if (!validName(nm) || s2.length() > MAX_NAME) {
                    say(p, "message.storagenet.net_bad_name", Integer.valueOf(MAX_NAME));
                    return;
                }
                FluxNetwork ex = f.byName(nm);
                if (ex == null) {
                    ex = f.create(nm, s2, uid);
                    ex.ownerName = p.getCommandSenderName();
                    say(p, "message.storagenet.net_created", ex.name);
                } else {
                    if (ex.level(uid) < 0) {
                        if (ex.password.length() > 0 && !ex.password.equals(s2)) {
                            say(p, "message.storagenet.net_wrong_pass");
                            return;
                        }
                        addMember(ex, p, 0);
                        f.markDirty();
                    }
                    say(p, "message.storagenet.net_joined", ex.name);
                }
                select(p, ex);
                break;
            }
            case 1: { // entrar (id + senha)
                if (n == null) {
                    say(p, "message.storagenet.net_missing");
                    break;
                }
                if (lv < 0) {
                    if (n.password.length() > 0 && !n.password.equals(s2)) {
                        say(p, "message.storagenet.net_wrong_pass");
                        break;
                    }
                    addMember(n, p, 0);
                    f.markDirty();
                }
                select(p, n);
                say(p, "message.storagenet.net_joined", n.name);
                break;
            }
            case 2: // selecionar
                if (n != null && lv >= 0) select(p, n);
                break;
            case 3: // apagar (dono)
                if (n != null && lv == 2) {
                    f.nets.remove(Integer.valueOf(n.id));
                    f.markDirty();
                    clearSel(p, n.id);
                    say(p, "message.storagenet.net_deleted", n.name);
                }
                break;
            case 4: { // renomear
                if (n == null || lv < 1) break;
                String nm = s1.trim();
                FluxNetwork o = f.byName(nm);
                if (!validName(nm) || (o != null && o != n)) {
                    say(p, "message.storagenet.net_bad_name", Integer.valueOf(MAX_NAME));
                    break;
                }
                n.name = nm;
                f.markDirty();
                ItemStack h = p.getHeldItem();
                if (h != null && h.getItem() instanceof ItemFluxConfigurator && ItemFluxConfigurator.netId(h) == n.id) select(p, n);
                break;
            }
            case 5: // senha
                if (n != null && lv >= 1 && s1.length() <= MAX_NAME) {
                    n.password = s1;
                    f.markDirty();
                }
                break;
            case 6: // cor
                if (n != null && lv >= 1) {
                    n.color = r.i2 & 0xFFFFFF;
                    f.markDirty();
                }
                break;
            case 7: { // membros: i2 = novo nivel (-1 expulsar)
                if (n == null || lv < 1 || s1.length() == 0 || s1.equals(n.owner)) break;
                Integer tl = n.members.get(s1);
                if (tl == null) break;
                if (r.i2 < 0) {
                    if (lv == 2 || tl.intValue() == 0) {
                        n.members.remove(s1);
                        n.memberNames.remove(s1);
                    }
                } else if (lv == 2) {
                    n.members.put(s1, Integer.valueOf(Math.min(1, r.i2)));
                }
                f.markDirty();
                break;
            }
            case 8: // sair
                if (n != null && lv >= 0 && lv < 2) {
                    n.members.remove(uid);
                    n.memberNames.remove(uid);
                    f.markDirty();
                    clearSel(p, n.id);
                }
                break;
            case 9: { // criar nova
                String nm = s1.trim();
                if (!validName(nm) || s2.length() > MAX_NAME) {
                    say(p, "message.storagenet.net_bad_name", Integer.valueOf(MAX_NAME));
                    break;
                }
                if (f.byName(nm) != null) {
                    say(p, "message.storagenet.net_exists");
                    break;
                }
                FluxNetwork c = f.create(nm, s2, uid);
                c.ownerName = p.getCommandSenderName();
                c.color = r.i2 & 0xFFFFFF;
                f.markDirty();
                select(p, c);
                say(p, "message.storagenet.net_created", c.name);
                break;
            }
            default:
                break;
        }
        sync(p);
    }

    private static void select(EntityPlayerMP p, FluxNetwork n) {
        ItemStack s = p.getHeldItem();
        if (s == null || !(s.getItem() instanceof ItemFluxConfigurator)) return;
        NBTTagCompound t = s.getTagCompound();
        if (t == null) {
            t = new NBTTagCompound();
            s.setTagCompound(t);
        }
        t.setInteger("Net", n.id);
        t.setString("NetName", n.name);
    }

    private static void clearSel(EntityPlayerMP p, int id) {
        ItemStack s = p.getHeldItem();
        if (s == null || !(s.getItem() instanceof ItemFluxConfigurator)) return;
        if (ItemFluxConfigurator.netId(s) != id) return;
        NBTTagCompound t = s.getTagCompound();
        if (t == null) return;
        t.setInteger("Net", 0);
        t.setString("NetName", "");
    }

    public static void sync(EntityPlayerMP p) {
        FluxNetworks f = get();
        String uid = p.getUniqueID().toString();
        MessageFluxSync m = new MessageFluxSync();
        m.sel = ItemFluxConfigurator.netId(p.getHeldItem());
        for (FluxNetwork n : f.nets.values()) m.nets.add(MessageFluxSync.Entry.of(n, uid));
        StorageNet.NET.sendTo(m, p);
    }

    public static void say(EntityPlayerMP p, String key, Object... args) {
        p.addChatMessage(new ChatComponentTranslation(key, args));
    }
}
