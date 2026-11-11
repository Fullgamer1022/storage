package com.storagenet;

/** Link Cable: liga os inventarios encostados a rede. Tem filtro, prioridade e direcao (IN/OUT/BOTH). */
public class TileLinkCable extends TileFilter {
    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public boolean isStorage() {
        return true;
    }
}
