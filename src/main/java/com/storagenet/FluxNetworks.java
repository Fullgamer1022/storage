package com.storagenet;

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
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * Lista de redes Flux (nome + senha), salva no mundo. Os pedidos da tela chegam por rede
 * (thread do netty), entram na fila e sao tratados no tick do servidor.
 */
public class FluxNetworks extends WorldSavedData {
    public static final String ID = "storagenet_flux_networks";
    public static final int MAX_NAME = 24;
    public static final Queue<Request> QUEUE = new ConcurrentLinkedQueue<Request>();

    private final Map<Integer, FluxNetwork> nets = new LinkedHashMap<Integer, FluxNetwork>();
    private int nextId = 1;

    public FluxNetworks() {
        super(ID);
    }

    public FluxNetworks(String name) {
        super(name);
    }

    public static class Request {
        public final EntityPlayerMP player;
        public final String name;
        public final String pass;

        public Request(EntityPlayerMP player, String name, String pass) {
            this.player = player;
            this.name = name;
            this.pass = pass;
        }
    }

    public static FluxNetworks get() {
        World w = MinecraftServer.getServer().worldServers[0];
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
            nets.put(Integer.valueOf(id), new FluxNetwork(id, t.getString("Name"), t.getString("Pass"), t.getString("Owner")));
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
            list.appendTag(t);
        }
        tag.setTag("Nets", list);
    }

    /** Chamado todo tick no servidor (FluxManager). */
    public static void processQueue() {
        Request r;
        while ((r = QUEUE.poll()) != null) {
            handle(r);
        }
    }

    private static void handle(Request r) {
        ItemStack held = r.player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemFluxConfigurator)) return;
        String name = r.name == null ? "" : r.name.trim();
        String pass = r.pass == null ? "" : r.pass;
        if (name.length() == 0 || name.length() > MAX_NAME || pass.length() > MAX_NAME) {
            say(r.player, "message.storagenet.net_bad_name", Integer.valueOf(MAX_NAME));
            return;
        }
        FluxNetworks data = get();
        FluxNetwork n = data.byName(name);
        if (n == null) {
            n = data.create(name, pass, r.player.getUniqueID().toString());
            say(r.player, "message.storagenet.net_created", n.name);
        } else if (!n.password.equals(pass)) {
            say(r.player, "message.storagenet.net_wrong_pass");
            return;
        } else {
            say(r.player, "message.storagenet.net_joined", n.name);
        }
        NBTTagCompound t = held.getTagCompound();
        if (t == null) {
            t = new NBTTagCompound();
            held.setTagCompound(t);
        }
        t.setInteger("Net", n.id);
        t.setString("NetName", n.name);
    }

    public static void say(EntityPlayerMP p, String key, Object... args) {
        p.addChatMessage(new ChatComponentTranslation(key, args));
    }
}
