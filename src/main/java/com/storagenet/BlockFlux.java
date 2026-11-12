package com.storagenet;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Blocos de energia sem fio (Plug, Point, Storage). Clique direito com um corante muda a frequencia. */
public class BlockFlux extends BlockContainer {
    /** Cores dos 16 corantes (indice = dano do corante). */
    private static final int[] COLORS = {
            0x333333, 0xD62828, 0x3E8E41, 0x7A4A22, 0x2F57D6, 0x8A3FC8, 0x1FA3A3, 0xB5B5B5,
            0x6E6E6E, 0xF58FB5, 0x7FE02B, 0xF2D722, 0x6FA9FF, 0xD65FD6, 0xF58A1F, 0xFFFFFF};

    private final Class<? extends TileEntity> tileClass;

    public BlockFlux(String name, Class<? extends TileEntity> tileClass) {
        super(Material.iron);
        this.tileClass = tileClass;
        setHardness(3.0F);
        setResistance(15.0F);
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

    @Override
    public int colorMultiplier(IBlockAccess w, int x, int y, int z) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof TileFlux) return COLORS[((TileFlux) te).freq & 15];
        return 0xFFFFFF;
    }

    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy, float hz) {
        if (w.isRemote) return true;
        TileEntity te = w.getTileEntity(x, y, z);
        if (!(te instanceof TileFlux)) return false;
        TileFlux t = (TileFlux) te;
        ItemStack held = p.getHeldItem();
        if (held != null && held.getItem() == Items.dye) {
            t.freq = held.getItemDamage() & 15;
            t.markDirty();
            w.markBlockForUpdate(x, y, z);
            p.addChatMessage(new ChatComponentTranslation("message.storagenet.flux_freq",
                    new ChatComponentTranslation("message.storagenet.color." + t.freq)));
            return true;
        }
        p.addChatMessage(new ChatComponentTranslation("message.storagenet.flux_freq",
                new ChatComponentTranslation("message.storagenet.color." + t.freq)));
        if (t instanceof TileFluxStorage) {
            TileFluxStorage s = (TileFluxStorage) t;
            p.addChatMessage(new ChatComponentText(s.stored + " / " + TileFluxStorage.CAPACITY + " RF"));
        }
        return true;
    }
}
