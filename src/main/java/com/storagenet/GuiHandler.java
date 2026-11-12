package com.storagenet;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class GuiHandler implements IGuiHandler {
    public static final int GRID = 0;
    public static final int CABLE = 1;
    public static final int REMOTE = 2;
    public static final int NETWORK = 3;
    public static final int FLUX_TILE = 4;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GRID) return new ContainerGrid(player, world, x, y, z);
        if (id == REMOTE) return new ContainerGrid(player);
        if (id == NETWORK || id == FLUX_TILE) return new ContainerNetwork();
        if (id == CABLE) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileFilter) return new ContainerCable(player, (TileFilter) te);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GRID) return new GuiGrid(new ContainerGrid(player, world, x, y, z));
        if (id == REMOTE) return new GuiGrid(new ContainerGrid(player));
        if (id == NETWORK) return new GuiNetwork(new ContainerNetwork());
        if (id == FLUX_TILE) return new GuiFluxTile(new ContainerNetwork(), x, y, z);
        if (id == CABLE) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileFilter) return new GuiCable(new ContainerCable(player, (TileFilter) te));
        }
        return null;
    }
}
