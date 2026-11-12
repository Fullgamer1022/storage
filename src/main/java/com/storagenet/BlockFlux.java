package com.storagenet;

import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Blocos de energia sem fio (Plug, Point, Storage). Clique direito com um corante muda a frequencia. */
public class BlockFlux extends BlockContainer {
    public static final int PLUG = 0;
    public static final int POINT = 1;
    public static final int STORAGE = 2;

    /** Cores dos 16 corantes (indice = dano do corante). */
    public static final int[] COLORS = {
            0x333333, 0xD62828, 0x3E8E41, 0x7A4A22, 0x2F57D6, 0x8A3FC8, 0x1FA3A3, 0xB5B5B5,
            0x6E6E6E, 0xF58FB5, 0x7FE02B, 0xF2D722, 0x6FA9FF, 0xD65FD6, 0xF58A1F, 0xFFFFFF};

    private final Class<? extends TileEntity> tileClass;
    public final int kind;

    @SideOnly(Side.CLIENT)
    public IIcon[] icons;

    public BlockFlux(String name, Class<? extends TileEntity> tileClass, int kind) {
        super(Material.iron);
        this.tileClass = tileClass;
        this.kind = kind;
        setHardness(3.0F);
        setResistance(15.0F);
        setStepSound(soundTypeMetal);
        setBlockName(StorageNet.MODID + "." + name);
        setCreativeTab(StorageNet.tab);
        setLightOpacity(0);
        useNeighborBrightness = true;
        if (kind != STORAGE) {
            float lo = coreMin();
            setBlockBounds(lo, lo, lo, 1.0F - lo, 1.0F - lo, 1.0F - lo);
        }
    }

    private float coreMin() {
        return kind == PLUG ? 0.25F : 0.34375F;
    }

    @Override
    public TileEntity createNewTileEntity(World w, int meta) {
        try {
            return tileClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- desenho ----------
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return StorageNet.fluxRenderId;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        icons = new IIcon[4];
        if (kind == STORAGE) {
            icons[0] = reg.registerIcon(StorageNet.MODID + ":flux_gray");
            icons[1] = reg.registerIcon(StorageNet.MODID + ":flux_tier1");
            icons[2] = reg.registerIcon(StorageNet.MODID + ":flux_lightblue");
            icons[3] = reg.registerIcon(StorageNet.MODID + ":flux_tier1_colour");
            blockIcon = icons[1];
        } else {
            String base = StorageNet.MODID + (kind == PLUG ? ":flux_plug" : ":flux_point");
            icons[0] = reg.registerIcon(base + "_on");
            icons[1] = reg.registerIcon(base + "_off");
            icons[3] = reg.registerIcon(base + "_colour");
            blockIcon = icons[0];
        }
    }

    public int tintColor(IBlockAccess w, int x, int y, int z) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof TileFlux) return COLORS[((TileFlux) te).freq & 15];
        return COLORS[12];
    }

    /** Ha um braco de conexao nesse lado (vizinho que fornece/recebe energia)? */
    public boolean hasArm(IBlockAccess w, int x, int y, int z, int side) {
        if (kind == STORAGE) return false;
        TileEntity te = w.getTileEntity(x + StorageNetwork.DX[side], y + StorageNetwork.DY[side],
                z + StorageNetwork.DZ[side]);
        if (te == null || te instanceof TileFlux) return false;
        return kind == PLUG ? te instanceof IEnergyProvider : te instanceof IEnergyReceiver;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess w, int x, int y, int z) {
        if (kind == STORAGE) {
            setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            return;
        }
        float lo = coreMin();
        float hi = 1.0F - lo;
        float x0 = lo, y0 = lo, z0 = lo, x1 = hi, y1 = hi, z1 = hi;
        if (hasArm(w, x, y, z, 0)) y0 = 0.0F;
        if (hasArm(w, x, y, z, 1)) y1 = 1.0F;
        if (hasArm(w, x, y, z, 2)) z0 = 0.0F;
        if (hasArm(w, x, y, z, 3)) z1 = 1.0F;
        if (hasArm(w, x, y, z, 4)) x0 = 0.0F;
        if (hasArm(w, x, y, z, 5)) x1 = 1.0F;
        setBlockBounds(x0, y0, z0, x1, y1, z1);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
        setBlockBoundsBasedOnState(w, x, y, z);
        return super.getCollisionBoundingBoxFromPool(w, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World w, int x, int y, int z, Block neighbor) {
        w.markBlockForUpdate(x, y, z);
    }

    // ---------- uso ----------
    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy, float hz) {
        if (w.isRemote) return true;
        TileEntity te = w.getTileEntity(x, y, z);
        if (!(te instanceof TileFlux)) return false;
        TileFlux t = (TileFlux) te;
        ItemStack held = p.getHeldItem();
        if (held != null && held.getItem() == Items.dye) {
            t.freq = held.getItemDamage() & 15;
            t.network = 0;
            t.markDirty();
            w.markBlockForUpdate(x, y, z);
        }
        if (t.network > 0) {
            FluxNetwork net = FluxNetworks.get().byId(t.network);
            if (net != null) p.addChatMessage(new ChatComponentTranslation("message.storagenet.net_block", net.name));
        }
        p.addChatMessage(new ChatComponentTranslation("message.storagenet.flux_freq",
                new ChatComponentTranslation("message.storagenet.color." + t.freq)));
        if (t instanceof TileFluxStorage && !(held != null && held.getItem() == Items.dye)) {
            TileFluxStorage s = (TileFluxStorage) t;
            p.addChatMessage(new ChatComponentText(s.stored + " / " + TileFluxStorage.CAPACITY + " RF"));
        }
        return true;
    }
}
