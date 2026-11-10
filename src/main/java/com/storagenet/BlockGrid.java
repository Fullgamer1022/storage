package com.storagenet;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockGrid extends BlockContainer {
    public BlockGrid() {
        super(Material.iron);
        setHardness(2.5F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setBlockName(StorageNet.MODID + ".grid");
        setBlockTextureName(StorageNet.MODID + ":grid");
        setCreativeTab(StorageNet.tab);
    }

    @Override
    public TileEntity createNewTileEntity(World w, int meta) {
        return new TileGrid();
    }

    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy, float hz) {
        if (!w.isRemote) p.openGui(StorageNet.instance, GuiHandler.GRID, w, x, y, z);
        return true;
    }
}
