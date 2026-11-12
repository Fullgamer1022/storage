package com.storagenet;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Cliente -> servidor: criar ou entrar numa rede Flux (nome + senha). */
public class MessageNetwork implements IMessage {
    public String name = "";
    public String pass = "";

    public MessageNetwork() {
    }

    public MessageNetwork(String name, String pass) {
        this.name = name;
        this.pass = pass;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, name);
        ByteBufUtils.writeUTF8String(buf, pass);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        name = ByteBufUtils.readUTF8String(buf);
        pass = ByteBufUtils.readUTF8String(buf);
    }

    public static class Handler implements IMessageHandler<MessageNetwork, IMessage> {
        @Override
        public IMessage onMessage(MessageNetwork m, MessageContext ctx) {
            // tratado na thread principal do servidor (FluxNetworks.processQueue)
            FluxNetworks.QUEUE.add(new FluxNetworks.Request(ctx.getServerHandler().playerEntity, m.name, m.pass));
            return null;
        }
    }
}
