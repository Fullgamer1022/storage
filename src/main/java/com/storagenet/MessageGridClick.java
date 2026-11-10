package com.storagenet;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Cliente -> servidor: clique na Grid. */
public class MessageGridClick implements IMessage {
    public static final int TAKE_STACK = 0;
    public static final int TAKE_HALF = 1;
    public static final int TAKE_TO_INV = 2;
    public static final int INSERT_ALL = 3;
    public static final int INSERT_ONE = 4;
    public static final int REFRESH = 5;

    public int mode;
    public boolean hasProto;
    public int id;
    public int dmg;
    public NBTTagCompound tag;

    public MessageGridClick() {
    }

    public MessageGridClick(int mode, StackEntry e) {
        this.mode = mode;
        if (e != null) {
            hasProto = true;
            id = e.id;
            dmg = e.dmg;
            tag = e.tag;
        }
    }

    public ItemStack buildProto() {
        if (!hasProto) return null;
        Item item = Item.getItemById(id);
        if (item == null) return null;
        ItemStack s = new ItemStack(item, 1, dmg);
        if (tag != null) s.setTagCompound((NBTTagCompound) tag.copy());
        return s;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(mode);
        buf.writeBoolean(hasProto);
        if (hasProto) {
            buf.writeInt(id);
            buf.writeInt(dmg);
            buf.writeBoolean(tag != null);
            if (tag != null) ByteBufUtils.writeTag(buf, tag);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mode = buf.readByte();
        hasProto = buf.readBoolean();
        if (hasProto) {
            id = buf.readInt();
            dmg = buf.readInt();
            if (buf.readBoolean()) tag = ByteBufUtils.readTag(buf);
        }
    }

    public static class Handler implements IMessageHandler<MessageGridClick, IMessage> {
        @Override
        public IMessage onMessage(MessageGridClick m, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().playerEntity;
            if (p.openContainer instanceof ContainerGrid) {
                // processado na thread principal, dentro do detectAndSendChanges
                ((ContainerGrid) p.openContainer).queueClick(m);
            }
            return null;
        }
    }
}
