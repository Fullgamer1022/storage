package com.storagenet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/** Base dos blocos de energia sem fio. A "frequencia" (0-15) e a cor do corante. */
public abstract class TileFlux extends TileEntity {
    /** Maximo de RF por tick que cada bloco move. */
    public static final int LIMIT = 1000000;

    public int freq = 0;

    /** Quanto de energia este bloco pode entregar a rede agora (sem tirar nada). */
    public abstract int available();

    /** Tira de verdade ate n RF e retorna quanto saiu. */
    public abstract int take(int n);

    /** Quanto de energia este bloco aceita agora (sem colocar nada). */
    public abstract int demand();

    /** Coloca de verdade ate n RF e retorna quanto entrou. */
    public abstract int give(int n);

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void validate() {
        super.validate();
        if (worldObj != null && !worldObj.isRemote) FluxManager.add(this);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        FluxManager.remove(this);
    }

    @Override
    public void onChunkUnload() {
        FluxManager.remove(this);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        freq = nbt.getInteger("Freq") & 15;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("Freq", freq);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound t = new NBTTagCompound();
        writeToNBT(t);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, t);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
}
