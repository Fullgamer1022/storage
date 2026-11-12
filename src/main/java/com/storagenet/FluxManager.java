package com.storagenet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class FluxManager {
    public static final List<TileFlux> ALL = new ArrayList<TileFlux>();

    private static final Comparator<TileFlux> BY_PRIO = new Comparator<TileFlux>() {
        public int compare(TileFlux a, TileFlux b) {
            return b.priority - a.priority;
        }
    };

    public static void add(TileFlux t) {
        if (!ALL.contains(t)) ALL.add(t);
    }

    public static void remove(TileFlux t) {
        ALL.remove(t);
    }

    public static void clear() {
        ALL.clear();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        FluxNetworks.processQueue();
        if (ALL.isEmpty()) return;

        Map<Integer, List<TileFlux>> groups = new TreeMap<Integer, List<TileFlux>>();
        for (TileFlux t : new ArrayList<TileFlux>(ALL)) {
            if (t.isInvalid() || t.getWorldObj() == null) continue;
            Integer k = Integer.valueOf(t.key());
            List<TileFlux> l = groups.get(k);
            if (l == null) {
                l = new ArrayList<TileFlux>();
                groups.put(k, l);
            }
            l.add(t);
        }

        FluxNetworks nets = null;
        for (Map.Entry<Integer, List<TileFlux>> g : groups.entrySet()) {
            int key = g.getKey().intValue();
            FluxNetwork net = null;
            if (key >= 16) {
                if (nets == null) nets = FluxNetworks.get();
                net = nets.byId(key - 16);
                if (net == null) continue; // rede apagada: nao transfere
            }
            List<TileFlux> plugs = new ArrayList<TileFlux>();
            List<TileFlux> points = new ArrayList<TileFlux>();
            List<TileFlux> stores = new ArrayList<TileFlux>();
            long stored = 0;
            for (TileFlux t : g.getValue()) {
                if (t instanceof TileFluxPlug) plugs.add(t);
                else if (t instanceof TileFluxPoint || t instanceof TileFluxController) points.add(t);
                else if (t instanceof TileFluxStorage) {
                    stores.add(t);
                    stored += ((TileFluxStorage) t).stored;
                }
            }
            if (net != null) {
                net.plugs = plugs.size();
                net.points = points.size();
                net.storages = stores.size();
                net.stored = stored;
            }
            if (plugs.isEmpty() && stores.isEmpty()) {
                if (net != null) net.rate = 0;
                continue;
            }
            Collections.sort(plugs, BY_PRIO);
            Collections.sort(points, BY_PRIO);
            Collections.sort(stores, BY_PRIO);
            int moved = transfer(plugs, points);
            transfer(plugs, stores);
            moved += transfer(stores, points);
            if (net != null) net.rate = moved;
        }
    }

    /** Move energia das fontes para os destinos respeitando prioridade e limite de cada bloco. */
    private static int transfer(List<TileFlux> src, List<TileFlux> dst) {
        if (src.isEmpty() || dst.isEmpty()) return 0;
        int total = 0;
        for (TileFlux d : dst) {
            int need = Math.min(d.demand(), d.maxTransfer());
            if (need <= 0) continue;
            for (TileFlux s : src) {
                if (need <= 0) break;
                int av = Math.min(s.available(), s.maxTransfer());
                if (av <= 0) continue;
                int got = s.take(Math.min(need, av));
                if (got <= 0) continue;
                int given = d.give(got);
                need -= given;
                total += given;
            }
        }
        return total;
    }
}
