package com.storagenet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * A rede e o conjunto de blocos INetworkNode ligados lado a lado.
 * Ela so funciona com exatamente UM Master. Os baus (inventarios) entram na rede
 * quando encostam num Link Cable.
 */
public class StorageNetwork {
    private static final int MAX_NODES = 2048;
    // indice = lado: 0 baixo, 1 cima, 2 norte(z-), 3 sul(z+), 4 oeste(x-), 5 leste(x+)
    public static final int[] DX = {0, 0, 0, 0, -1, 1};
    public static final int[] DY = {-1, 1, 0, 0, 0, 0};
    public static final int[] DZ = {0, 0, -1, 1, 0, 0};

    /** Criterio para achar itens na rede (usado pelo Export Cable). */
    public interface Matcher {
        boolean matches(ItemStack s);
    }

    public static class Net {
        public int masters;
        public final List<InvRef> invs = new ArrayList<InvRef>();

        public boolean valid() {
            return masters == 1;
        }

        /** 0 = ok, 1 = sem Master, 2 = Master demais. */
        public int status() {
            return masters == 0 ? 1 : (masters > 1 ? 2 : 0);
        }

        /** Retorna o que sobrou (null se coube tudo). 'exclude' = inventario a ignorar. */
        public ItemStack insert(ItemStack stack, IInventory exclude) {
            ItemStack rest = stack;
            for (InvRef r : invs) {
                if (r.inv == exclude) continue;
                if (!r.allowsIn(rest)) continue;
                rest = r.insert(rest);
                if (rest == null) return null;
            }
            return rest;
        }

        /** Quantos itens iguais a 'proto' existem na rede (so conta o que da para tirar). */
        public int amount(ItemStack proto) {
            int total = 0;
            for (InvRef r : invs) {
                if (!r.allowsOut(proto)) continue;
                total += r.count(proto);
            }
            return total;
        }

        /** Primeiro item da rede que passa em 'm' (copia com 1 unidade) ou null. */
        public ItemStack find(Matcher m, IInventory exclude) {
            for (InvRef r : invs) {
                if (r.inv == exclude) continue;
                ItemStack f = r.find(m);
                if (f != null) return f;
            }
            return null;
        }

        public ItemStack extract(ItemStack proto, int amount, IInventory exclude) {
            int total = 0;
            for (InvRef r : invs) {
                if (r.inv == exclude) continue;
                if (!r.allowsOut(proto)) continue;
                total += r.extract(proto, amount - total);
                if (total >= amount) break;
            }
            if (total <= 0) return null;
            ItemStack out = proto.copy();
            out.stackSize = total;
            return out;
        }

        public List<StackEntry> list() {
            Map<String, StackEntry> map = new LinkedHashMap<String, StackEntry>();
            for (InvRef r : invs) r.collect(map);
            return new ArrayList<StackEntry>(map.values());
        }
    }

    private static long key(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }

    public static Net scan(World w, int sx, int sy, int sz) {
        Net net = new Net();
        if (sy < 0 || sy > 255 || !w.blockExists(sx, sy, sz)) return net;
        Set<Long> seen = new HashSet<Long>();
        Set<Long> invSeen = new HashSet<Long>();
        LinkedList<int[]> queue = new LinkedList<int[]>();
        seen.add(key(sx, sy, sz));
        queue.add(new int[]{sx, sy, sz});
        while (!queue.isEmpty()) {
            int[] p = queue.removeFirst();
            TileEntity here = w.getTileEntity(p[0], p[1], p[2]);
            if (here instanceof TileMaster) net.masters++;
            boolean link = here instanceof TileLinkCable;
            for (int i = 0; i < 6; i++) {
                int nx = p[0] + DX[i];
                int ny = p[1] + DY[i];
                int nz = p[2] + DZ[i];
                if (ny < 0 || ny > 255 || !w.blockExists(nx, ny, nz)) continue;
                TileEntity te = w.getTileEntity(nx, ny, nz);
                if (te instanceof INetworkNode) {
                    if (seen.size() >= MAX_NODES) continue;
                    if (seen.add(key(nx, ny, nz))) queue.add(new int[]{nx, ny, nz});
                } else if (link && te instanceof IInventory) {
                    addInv(w, net, invSeen, nx, ny, nz, i ^ 1, (TileFilter) here);
                }
            }
        }
        // prioridade maior primeiro (a ordem entre iguais e mantida)
        Collections.sort(net.invs, new Comparator<InvRef>() {
            @Override
            public int compare(InvRef a, InvRef b) {
                return b.priority() - a.priority();
            }
        });
        return net;
    }

    private static void addInv(World w, Net net, Set<Long> invSeen, int x, int y, int z, int side, TileFilter link) {
        if (!invSeen.add(key(x, y, z))) return;
        net.invs.add(new InvRef((IInventory) w.getTileEntity(x, y, z), side, link));
        // bau duplo: a outra metade tambem entra
        Block b = w.getBlock(x, y, z);
        if (b == Blocks.chest || b == Blocks.trapped_chest) {
            for (int i = 2; i < 6; i++) {
                int nx = x + DX[i];
                int nz = z + DZ[i];
                if (w.getBlock(nx, y, nz) == b) {
                    TileEntity t2 = w.getTileEntity(nx, y, nz);
                    if (t2 instanceof IInventory && invSeen.add(key(nx, y, nz))) {
                        net.invs.add(new InvRef((IInventory) t2, 1, link));
                    }
                }
            }
        }
    }

    /** Inventario nessa posicao, se existir e nao for da propria rede. */
    public static IInventory invAt(World w, int x, int y, int z) {
        if (y < 0 || y > 255 || !w.blockExists(x, y, z)) return null;
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof IInventory && !(te instanceof INetworkNode)) return (IInventory) te;
        return null;
    }
}
