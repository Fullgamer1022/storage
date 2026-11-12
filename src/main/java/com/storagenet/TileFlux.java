package com.storagenet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public abstract class TileFlux extends TileEntity {
    public static final int LIMIT = 1000000;

    public int freq;
    public int network;
    public int priority;
    public int limit = LIMIT;
    public boolean unlimited;
    public String customName = "";

    public int key() {
        return network > 0 ? 16 + network : freq;
    }

    public int maxTransfer() {
        return unlimited ? LIMIT : Math.max(0, Math.min(limit, LIMIT));
    }

    public void setConfig(String name, int prio, int lim, boolean unl) {
        customName = name == null ? "" : (name.length() > 24 ? name.substring(0, 24) : name);
        priority = Math.max(-99, Math.min(99, prio));
        limit = Math.max(0, Math.min(LIMIT, lim));
        unlimited = unl;
    }

    public abstract int available();

    public abstract int take(int amount);

    public abstract int demand();

    public abstract int give(int amount);

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
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        freq = Math.max(0, Math.min(15, tag.getInteger("Freq")));
        network = Math.max(0, tag.getInteger("Net"));
        priority = Math.max(-99, Math.min(99, tag.getInteger("Prio")));
        limit = tag.hasKey("Limit") ? Math.max(0, Math.min(LIMIT, tag.getInteger("Limit"))) : LIMIT;
        unlimited = tag.getBoolean("Unl");
        customName = tag.getString("CName");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Freq", freq);
        tag.setInteger("Net", network);
        tag.setInteger("Prio", priority);
        tag.setInteger("Limit", limit);
        tag.setBoolean("Unl", unlimited);
        tag.setString("CName", customName == null ? "" : customName);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
}
