package com.storagenet;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;

/** Servidor -> cliente: lista de itens da rede (enviada em pedacos para nao passar de 32 KB). */
public class MessageGridList implements IMessage {
    public boolean first;
    public boolean last;
    public int status;
    public int sort;
    public boolean down;
    public List<StackEntry> entries = new ArrayList<StackEntry>();

    public MessageGridList() {
    }

    public MessageGridList(boolean first, boolean last, int status, int sort, boolean down, List<StackEntry> entries) {
        this.first = first;
        this.last = last;
        this.status = status;
        this.sort = sort;
        this.down = down;
        this.entries = entries;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(first);
        buf.writeBoolean(last);
        buf.writeByte(status);
        buf.writeByte(sort);
        buf.writeBoolean(down);
        buf.writeInt(entries.size());
        for (StackEntry e : entries) e.write(buf);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        first = buf.readBoolean();
        last = buf.readBoolean();
        status = buf.readByte();
        sort = buf.readByte();
        down = buf.readBoolean();
        int n = buf.readInt();
        entries = new ArrayList<StackEntry>();
        for (int i = 0; i < n; i++) entries.add(StackEntry.read(buf));
    }

    public static class Handler implements IMessageHandler<MessageGridList, IMessage> {
        @Override
        public IMessage onMessage(MessageGridList m, MessageContext ctx) {
            EntityPlayer p = FMLClientHandler.instance().getClientPlayerEntity();
            if (p != null && p.openContainer instanceof ContainerGrid) {
                ((ContainerGrid) p.openContainer).receive(m);
            }
            return null;
        }
    }
}
