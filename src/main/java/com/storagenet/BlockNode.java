package com.storagenet;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Bloco generico da rede (Master, cabos...). Cada um cria o seu TileEntity. */
public class BlockNode extends BlockContainer {
    private final Class<? extends TileEntity> tileClass;

    public BlockNode(String name, Class<? extends TileEntity> tileClass) {
        super(Material.iron);
        this.tileClass = tileClass;
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setBlockName(StorageNet.MODID + "." + name);
        setBlockTextureName(StorageNet.MODID + ":" + name);
        setCreativeTab(StorageNet.tab);
    }

    @Override
    public TileEntity createNewTileEntity(World w, int meta) {
        try {
            return tileClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
