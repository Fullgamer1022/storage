package com.storagenet;

import net.minecraft.tileentity.TileEntity;

/** Base dos blocos da rede que nao fazem nada sozinhos (nao precisam de tick). */
public class TileNode extends TileEntity implements INetworkNode {
    @Override
    public boolean canUpdate() {
        return false;
    }
}
