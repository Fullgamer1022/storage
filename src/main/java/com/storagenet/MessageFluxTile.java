package com.storagenet;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Cliente -> servidor: configuracao de um bloco Flux (nome, prioridade, limite, rede). */
public class MessageFluxTile implements IMessage {
    public int x, y, z, prio, limit, net;
    public boolean unl;
    public String name = "";

    public MessageFluxTile() {
    }

    public MessageFluxTile(int x, int y, int z, String name, int prio, int limit, boolean unl, int net) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.name = name == null ? "" : name;
        this.prio = prio;
        this.limit = limit;
        this.unl = unl;
        this.net = net;
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeInt(x);
        b.writeInt(y);
        b.writeInt(z);
        b.writeInt(prio);
        b.writeInt(limit);
        b.writeInt(net);
        b.writeBoolean(unl);
        ByteBufUtils.writeUTF8String(b, name);
    }

    @Override
    public void fromBytes(ByteBuf b) {
        x = b.readInt();
        y = b.readInt();
        z = b.readInt();
        prio = b.readInt();
        limit = b.readInt();
        net = b.readInt();
        unl = b.readBoolean();
        name = ByteBufUtils.readUTF8String(b);
    }

    public static class Handler implements IMessageHandler<MessageFluxTile, IMessage> {
        @Override
        public IMessage onMessage(final MessageFluxTile m, MessageContext ctx) {
            final EntityPlayerMP p = ctx.getServerHandler().playerEntity;
            FluxNetworks.TASKS.add(new Runnable() {
                public void run() {
                    World w = p.worldObj;
                    if (!w.blockExists(m.x, m.y, m.z)) return;
                    if (p.getDistanceSq(m.x + 0.5, m.y + 0.5, m.z + 0.5) > 64.0) return;
                    TileEntity te = w.getTileEntity(m.x, m.y, m.z);
                    if (!(te instanceof TileFlux)) return;
                    TileFlux t = (TileFlux) te;
                    String uid = p.getUniqueID().toString();
                    FluxNetworks f = FluxNetworks.get();
                    if (t.network > 0) {
                        FluxNetwork cur = f.byId(t.network);
                        if (cur != null && cur.level(uid) < 0) {
                            FluxNetworks.say(p, "message.storagenet.net_no_access");
                            return;
                        }
                    }
                    t.setConfig(m.name, m.prio, m.limit, m.unl);
                    if (m.net == 0) {
                        t.network = 0;
                    } else if (m.net > 0) {
                        FluxNetwork n = f.byId(m.net);
                        if (n != null && n.level(uid) >= 0) t.network = n.id;
                        else FluxNetworks.say(p, "message.storagenet.net_no_access");
                    }
                    t.markDirty();
                    w.markBlockForUpdate(m.x, m.y, m.z);
                    FluxNetworks.sync(p);
                }
            });
            return null;
        }
    }
}
