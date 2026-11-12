package com.storagenet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Servidor -> cliente: lista de redes Flux visiveis + rede selecionada no Configurator. */
public class MessageFluxSync implements IMessage {
    public int sel;
    public List<Entry> nets = new ArrayList<Entry>();

    public static class Entry {
        public int id, color, level, count, rate, plugs, points, storages;
        public long stored;
        public boolean locked;
        public String name = "", owner = "";
        public List<String> mId = new ArrayList<String>();
        public List<String> mName = new ArrayList<String>();
        public List<Integer> mLv = new ArrayList<Integer>();

        public static Entry of(FluxNetwork n, String uid) {
            Entry e = new Entry();
            e.id = n.id;
            e.name = n.name;
            e.owner = n.ownerName == null ? "" : n.ownerName;
            e.color = n.color;
            e.locked = n.password.length() > 0;
            e.level = n.level(uid);
            e.count = n.members.size() + 1;
            e.rate = n.rate;
            e.plugs = n.plugs;
            e.points = n.points;
            e.storages = n.storages;
            e.stored = n.stored;
            if (e.level >= 1) {
                e.mId.add(n.owner);
                e.mName.add(e.owner);
                e.mLv.add(Integer.valueOf(2));
                for (Map.Entry<String, Integer> m : n.members.entrySet()) {
                    e.mId.add(m.getKey());
                    String nm = n.memberNames.get(m.getKey());
                    e.mName.add(nm == null ? "?" : nm);
                    e.mLv.add(m.getValue());
                }
            }
            return e;
        }

        void write(ByteBuf b) {
            b.writeInt(id);
            b.writeInt(color);
            b.writeInt(level);
            b.writeInt(count);
            b.writeInt(rate);
            b.writeInt(plugs);
            b.writeInt(points);
            b.writeInt(storages);
            b.writeLong(stored);
            b.writeBoolean(locked);
            ByteBufUtils.writeUTF8String(b, name);
            ByteBufUtils.writeUTF8String(b, owner);
            b.writeInt(mId.size());
            for (int i = 0; i < mId.size(); i++) {
                ByteBufUtils.writeUTF8String(b, mId.get(i));
                ByteBufUtils.writeUTF8String(b, mName.get(i));
                b.writeInt(mLv.get(i).intValue());
            }
        }

        static Entry read(ByteBuf b) {
            Entry e = new Entry();
            e.id = b.readInt();
            e.color = b.readInt();
            e.level = b.readInt();
            e.count = b.readInt();
            e.rate = b.readInt();
            e.plugs = b.readInt();
            e.points = b.readInt();
            e.storages = b.readInt();
            e.stored = b.readLong();
            e.locked = b.readBoolean();
            e.name = ByteBufUtils.readUTF8String(b);
            e.owner = ByteBufUtils.readUTF8String(b);
            int c = b.readInt();
            for (int i = 0; i < c; i++) {
                e.mId.add(ByteBufUtils.readUTF8String(b));
                e.mName.add(ByteBufUtils.readUTF8String(b));
                e.mLv.add(Integer.valueOf(b.readInt()));
            }
            return e;
        }
    }

    public MessageFluxSync() {
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeInt(sel);
        b.writeInt(nets.size());
        for (Entry e : nets) e.write(b);
    }

    @Override
    public void fromBytes(ByteBuf b) {
        sel = b.readInt();
        int c = b.readInt();
        nets = new ArrayList<Entry>();
        for (int i = 0; i < c; i++) nets.add(Entry.read(b));
    }

    public static class Handler implements IMessageHandler<MessageFluxSync, IMessage> {
        @Override
        public IMessage onMessage(MessageFluxSync m, MessageContext ctx) {
            FluxClient.nets = m.nets;
            FluxClient.sel = m.sel;
            return null;
        }
    }
}
