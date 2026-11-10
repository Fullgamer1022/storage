package com.storagenet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

/** Export Cable: clique direito com um item define o filtro. */
public class BlockExport extends BlockNode {
    public BlockExport() {
        super("export_cable", TileExport.class, BlockNode.CABLE_INV);
    }

    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy, float hz) {
        if (w.isRemote) return true;
        TileEntity te = w.getTileEntity(x, y, z);
        if (!(te instanceof TileExport)) return false;
        TileExport t = (TileExport) te;
        ItemStack held = p.getHeldItem();
        if (held != null) {
            ItemStack f = held.copy();
            f.stackSize = 1;
            t.setFilter(f);
            p.addChatMessage(new ChatComponentTranslation("message.storagenet.filter_set", f.getDisplayName()));
        } else if (p.isSneaking()) {
            t.setFilter(null);
            p.addChatMessage(new ChatComponentTranslation("message.storagenet.filter_cleared"));
        } else {
            ItemStack f = t.getFilter();
            if (f == null) {
                p.addChatMessage(new ChatComponentTranslation("message.storagenet.filter_none"));
            } else {
                p.addChatMessage(new ChatComponentTranslation("message.storagenet.filter_is", f.getDisplayName()));
            }
        }
        return true;
    }
}
