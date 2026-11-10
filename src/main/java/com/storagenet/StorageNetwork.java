package com.storagenet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** A "rede" e simplesmente o conjunto de blocos INetworkNode ligados lado a lado. */
public class StorageNetwork {
    private static final int MAX_NODES = 4096;
    private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private static long key(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }

    public static List<TileDrive> findDrives(World w, int sx, int sy, int sz) {
        List<TileDrive> drives = new ArrayList<TileDrive>();
        Set<Long> seen = new HashSet<Long>();
        LinkedList<int[]> queue = new LinkedList<int[]>();
        seen.add(key(sx, sy, sz));
        queue.add(new int[]{sx, sy, sz});
        while (!queue.isEmpty() && seen.size() < MAX_NODES) {
            int[] p = queue.removeFirst();
            for (int[] d : DIRS) {
                int nx = p[0] + d[0];
                int ny = p[1] + d[1];
                int nz = p[2] + d[2];
                if (ny < 0 || ny > 255) continue;
                if (!w.blockExists(nx, ny, nz)) continue;
                long k = key(nx, ny, nz);
                if (seen.contains(k)) continue;
                TileEntity te = w.getTileEntity(nx, ny, nz);
                if (te instanceof INetworkNode) {
                    seen.add(k);
                    queue.add(new int[]{nx, ny, nz});
                    if (te instanceof TileDrive) drives.add((TileDrive) te);
                }
            }
        }
        return drives;
    }

    /** Retorna o que sobrou (null se coube tudo). */
    public static ItemStack insert(List<TileDrive> drives, ItemStack stack) {
        ItemStack rest = stack;
        for (TileDrive d : drives) {
            rest = d.insert(rest);
            if (rest == null) return null;
        }
        return rest;
    }

    public static ItemStack extract(List<TileDrive> drives, ItemStack proto, int amount) {
        int total = 0;
        for (TileDrive d : drives) {
            total += d.extract(proto, amount - total);
            if (total >= amount) break;
        }
        if (total <= 0) return null;
        ItemStack out = proto.copy();
        out.stackSize = total;
        return out;
    }

    public static List<StackEntry> list(List<TileDrive> drives) {
        Map<String, StackEntry> map = new LinkedHashMap<String, StackEntry>();
        for (TileDrive d : drives) d.collect(map);
        return new ArrayList<StackEntry>(map.values());
    }
}
