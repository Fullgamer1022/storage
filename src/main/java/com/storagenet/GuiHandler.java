package com.storagenet;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class GuiHandler implements IGuiHandler {
    public static final int GRID = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
        if (id == GRID) return new ContainerGrid(p, w, x, y, z);
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
        if (id == GRID) return new GuiGrid(new ContainerGrid(p, w, x, y, z));
        return null;
    }
}
