package com.storagenet;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Um tipo de item guardado na rede, com a quantidade total (int, sem limite de 64). */
public class StackEntry {
    public int id;
    public int dmg;
    public int count;
    public NBTTagCompound tag;

    public StackEntry() {
    }

    public StackEntry(int id, int dmg, int count, NBTTagCompound tag) {
        this.id = id;
        this.dmg = dmg;
        this.count = count;
        this.tag = tag;
    }

    public ItemStack toStack(int size) {
        Item item = Item.getItemById(id);
        if (item == null) return null;
        ItemStack s = new ItemStack(item, size, dmg);
        if (tag != null) s.setTagCompound((NBTTagCompound) tag.copy());
        return s;
    }

    public boolean same(StackEntry o) {
        if (id != o.id || dmg != o.dmg) return false;
        if (tag == null) return o.tag == null;
        return o.tag != null && tag.equals(o.tag);
    }

    public void write(ByteBuf buf) {
        buf.writeInt(id);
        buf.writeInt(dmg);
        buf.writeInt(count);
        buf.writeBoolean(tag != null);
        if (tag != null) ByteBufUtils.writeTag(buf, tag);
    }

    public static StackEntry read(ByteBuf buf) {
        StackEntry e = new StackEntry();
        e.id = buf.readInt();
        e.dmg = buf.readInt();
        e.count = buf.readInt();
        if (buf.readBoolean()) e.tag = ByteBufUtils.readTag(buf);
        return e;
    }

    public int byteSize() {
        ByteBuf b = Unpooled.buffer();
        write(b);
        int n = b.readableBytes();
        b.release();
        return n;
    }
}
