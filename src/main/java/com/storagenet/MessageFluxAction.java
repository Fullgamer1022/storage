package com.storagenet;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Cliente -> servidor: acoes da GUI de redes (ver FluxNetworks.handle). */
public class MessageFluxAction implements IMessage {
    public int type, i1, i2;
    public String s1 = "", s2 = "";

    public MessageFluxAction() {
    }

    public MessageFluxAction(int type, String s1, String s2, int i1, int i2) {
        this.type = type;
        this.s1 = s1 == null ? "" : s1;
        this.s2 = s2 == null ? "" : s2;
        this.i1 = i1;
        this.i2 = i2;
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeInt(type);
        b.writeInt(i1);
        b.writeInt(i2);
        ByteBufUtils.writeUTF8String(b, s1);
        ByteBufUtils.writeUTF8String(b, s2);
    }

    @Override
    public void fromBytes(ByteBuf b) {
        type = b.readInt();
        i1 = b.readInt();
        i2 = b.readInt();
        s1 = ByteBufUtils.readUTF8String(b);
        s2 = ByteBufUtils.readUTF8String(b);
    }

    public static class Handler implements IMessageHandler<MessageFluxAction, IMessage> {
        @Override
        public IMessage onMessage(MessageFluxAction m, MessageContext ctx) {
            FluxNetworks.QUEUE.add(new FluxNetworks.Request(ctx.getServerHandler().playerEntity, m.type, m.s1, m.s2, m.i1, m.i2));
            return null;
        }
    }
}
