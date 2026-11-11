package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Bloco da rede. CUBE = bloco inteiro (Master, Request Table).
 * CABLE = cabo fino. CABLE_INV = cabo fino que tambem se liga a inventarios (com uma plaquinha colorida).
 */
public class BlockNode extends BlockContainer {
    public static final int CUBE = 0;
    public static final int CABLE = 1;
    public static final int CABLE_INV = 2;

    private final Class<? extends TileEntity> tileClass;
    private final int kind;
    private final String name;

    @SideOnly(Side.CLIENT)
    public IIcon plateIcon;

    public BlockNode(String name, Class<? extends TileEntity> tileClass, int kind) {
        super(Material.iron);
        this.name = name;
        this.tileClass = tileClass;
        this.kind = kind;
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setBlockName(StorageNet.MODID + "." + name);
        setCreativeTab(StorageNet.tab);
        if (kind != CUBE) setBlockBounds(0.3125F, 0.3125F, 0.3125F, 0.6875F, 0.6875F, 0.6875F);
    }

    public String getName() {
        return name;
    }

    /** Cabos com configuracao (Link, Import, Export): clique direito abre a tela de filtros. */
    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy, float hz) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (!(te instanceof TileFilter)) return false;
        if (!w.isRemote) p.openGui(StorageNet.instance, GuiHandler.CABLE, w, x, y, z);
        return true;
    }

    /** Nao perde os upgrades quando o cabo e quebrado. */
    @Override
    public void breakBlock(World w, int x, int y, int z, Block b, int meta) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof TileFilter && !w.isRemote) {
            for (ItemStack s : ((TileFilter) te).upgrades) {
                if (s != null && s.stackSize > 0) dropStack(w, x, y, z, s);
            }
        }
        super.breakBlock(w, x, y, z, b, meta);
    }

    public static void dropStack(World w, int x, int y, int z, ItemStack s) {
        w.spawnEntityInWorld(new EntityItem(w, x + 0.5, y + 0.5, z + 0.5, s.copy()));
    }

    @Override
    public TileEntity createNewTileEntity(World w, int meta) {
        try {
            return tileClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        if (kind == CUBE) {
            blockIcon = reg.registerIcon(StorageNet.MODID + ":" + name);
        } else {
            blockIcon = reg.registerIcon(StorageNet.MODID + ":cable");
            if (kind == CABLE_INV) plateIcon = reg.registerIcon(StorageNet.MODID + ":" + name);
        }
    }

    // ---------- forma e desenho ----------
    @Override
    public boolean isOpaqueCube() {
        return kind == CUBE;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return kind == CUBE;
    }

    @Override
    public int getRenderType() {
        return kind == CUBE ? 0 : StorageNet.cableRenderId;
    }

    public boolean connectsNode(IBlockAccess w, int x, int y, int z, int side) {
        return w.getTileEntity(x + StorageNetwork.DX[side], y + StorageNetwork.DY[side],
                z + StorageNetwork.DZ[side]) instanceof INetworkNode;
    }

    public boolean connectsInv(IBlockAccess w, int x, int y, int z, int side) {
        if (kind != CABLE_INV) return false;
        TileEntity te = w.getTileEntity(x + StorageNetwork.DX[side], y + StorageNetwork.DY[side],
                z + StorageNetwork.DZ[side]);
        return te instanceof IInventory && !(te instanceof INetworkNode);
    }

    private boolean connects(IBlockAccess w, int x, int y, int z, int side) {
        return connectsNode(w, x, y, z, side) || connectsInv(w, x, y, z, side);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess w, int x, int y, int z) {
        if (kind == CUBE) return;
        float x0 = 0.3125F, y0 = 0.3125F, z0 = 0.3125F, x1 = 0.6875F, y1 = 0.6875F, z1 = 0.6875F;
        if (connects(w, x, y, z, 0)) y0 = 0.0F;
        if (connects(w, x, y, z, 1)) y1 = 1.0F;
        if (connects(w, x, y, z, 2)) z0 = 0.0F;
        if (connects(w, x, y, z, 3)) z1 = 1.0F;
        if (connects(w, x, y, z, 4)) x0 = 0.0F;
        if (connects(w, x, y, z, 5)) x1 = 1.0F;
        setBlockBounds(x0, y0, z0, x1, y1, z1);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
        setBlockBoundsBasedOnState(w, x, y, z);
        return super.getCollisionBoundingBoxFromPool(w, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World w, int x, int y, int z, Block neighbor) {
        if (kind != CUBE) w.markBlockForUpdate(x, y, z);
    }
}
