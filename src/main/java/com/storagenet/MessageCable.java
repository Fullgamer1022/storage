package com.storagenet;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

/** Cliente -> servidor: tudo que se faz na tela de um cabo (prioridade, lista branca, filtros...). */
public class MessageCable implements IMessage {
    public static final int PRIORITY = 0;     // a = +1 / -1
    public static final int WHITE = 1;
    public static final int WAY = 2;
    public static final int MODE = 3;
    public static final int LIMIT = 4;        // a = novo limite
    public static final int IMPORT_FILTER = 5;
    public static final int GHOST = 6;        // a = slot, b = bit0: botao direito, bit1: shift
    public static final int ADJUST = 7;       // a = slot, b = variacao da quantidade
    public static final int OP_ITEM = 8;      // item monitorado (usa o item do cursor; vazio limpa)

    public int action;
    public int x, y, z;
    public int a, b;

    public MessageCable() {
    }

    public MessageCable(int action, TileFilter t, int a, int b) {
        this.action = action;
        this.x = t.xCoord;
        this.y = t.yCoord;
        this.z = t.zCoord;
        this.a = a;
        this.b = b;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(a);
        buf.writeInt(b);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        a = buf.readInt();
        b = buf.readInt();
    }

    public static class Handler implements IMessageHandler<MessageCable, IMessage> {
        @Override
        public IMessage onMessage(MessageCable m, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().playerEntity;
            if (p.openContainer instanceof ContainerCable) {
                // processado na thread principal, dentro do detectAndSendChanges
                ((ContainerCable) p.openContainer).queue(m);
            }
            return null;
        }
    }
}
