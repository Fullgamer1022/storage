package com.storagenet;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockCable extends BlockContainer {
    public BlockCable() {
        super(Material.iron);
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setBlockName(StorageNet.MODID + ".cable");
        setBlockTextureName(StorageNet.MODID + ":cable");
        setCreativeTab(StorageNet.tab);
    }

    @Override
    public TileEntity createNewTileEntity(World w, int meta) {
        return new TileCable();
    }
}
