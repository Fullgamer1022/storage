package com.storagenet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockDrive extends BlockContainer {
    public BlockDrive() {
        super(Material.iron);
        setHardness(2.5F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setBlockName(StorageNet.MODID + ".drive");
        setBlockTextureName(StorageNet.MODID + ":drive");
        setCreativeTab(StorageNet.tab);
    }

    @Override
    public TileEntity createNewTileEntity(World w, int meta) {
        return new TileDrive();
    }

    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy, float hz) {
        if (!w.isRemote) p.openGui(StorageNet.instance, GuiHandler.DRIVE, w, x, y, z);
        return true;
    }

    @Override
    public void breakBlock(World w, int x, int y, int z, Block block, int meta) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof TileDrive) ((TileDrive) te).dropContents();
        super.breakBlock(w, x, y, z, block, meta);
    }
}
