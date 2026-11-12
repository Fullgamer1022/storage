package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class BlockFluxController extends BlockContainer {
    @SideOnly(Side.CLIENT)
    private IIcon bottomIcon;

    public BlockFluxController() {
        super(Material.iron);
        setHardness(5.0F);
        setResistance(2000.0F);
        setStepSound(soundTypeMetal);
        setBlockName("storagenet.flux_controller");
        setBlockTextureName("storagenet:flux_controller");
        setCreativeTab(StorageNet.tab);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileFluxController();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon("storagenet:flux_controller");
        bottomIcon = reg.registerIcon("storagenet:flux_controller_bottom");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side == 0 ? bottomIcon : blockIcon;
    }
}
