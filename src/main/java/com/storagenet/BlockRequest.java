package com.storagenet;

import net.minecraft.entity.player.EntityPlayer;
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
}
