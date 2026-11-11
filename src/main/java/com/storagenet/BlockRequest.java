package com.storagenet;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Request Table: o terminal para ver e pegar os itens da rede. */
public class BlockRequest extends BlockNode {
    public BlockRequest() {
        super("request", TileRequest.class, BlockNode.CUBE);
    }

    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy, float hz) {
        if (!w.isRemote) p.openGui(StorageNet.instance, GuiHandler.GRID, w, x, y, z);
        return true;
    }

    /** O que ficou na grade de crafting cai no chao quando a mesa e quebrada. */
    @Override
    public void breakBlock(World w, int x, int y, int z, Block b, int meta) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof TileRequest && !w.isRemote) {
            for (ItemStack s : ((TileRequest) te).matrix) {
                if (s != null && s.stackSize > 0) dropStack(w, x, y, z, s);
            }
        }
        super.breakBlock(w, x, y, z, b, meta);
    }
}
