package com.storagenet;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Move a energia entre os blocos Flux de mesma frequencia (cor), em qualquer distancia
 * e qualquer dimensao. Roda uma vez por tick no servidor.
 */
public class FluxManager {
    private static final List<TileFlux> ALL = new ArrayList<TileFlux>();

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
        List<TileFlux> snapshot = new ArrayList<TileFlux>(ALL);
        java.util.Set<Integer> keys = new java.util.TreeSet<Integer>();
        for (TileFlux t : snapshot) keys.add(Integer.valueOf(t.key()));
        for (int f : keys) {
            List<TileFlux> plugs = new ArrayList<TileFlux>();
            List<TileFlux> points = new ArrayList<TileFlux>();
            List<TileFlux> stores = new ArrayList<TileFlux>();
            for (TileFlux t : snapshot) {
                if (t.key() != f || t.isInvalid() || t.getWorldObj() == null) continue;
                if (t instanceof TileFluxPlug) plugs.add(t);
                else if (t instanceof TileFluxPoint || t instanceof TileFluxController) points.add(t);
                else if (t instanceof TileFluxStorage) stores.add(t);
            }
            if (plugs.isEmpty() && stores.isEmpty()) continue;
            transfer(plugs, points);
            transfer(plugs, stores);
            transfer(stores, points);
        }
    }

    private static void transfer(List<TileFlux> sources, List<TileFlux> sinks) {
        if (sources.isEmpty() || sinks.isEmpty()) return;
        for (TileFlux sink : sinks) {
            int want = sink.demand();
            if (want <= 0) continue;
            for (TileFlux src : sources) {
                if (want <= 0) break;
                int can = src.available();
                if (can <= 0) continue;
                int got = src.take(Math.min(want, can));
                if (got <= 0) continue;
                want -= sink.give(got);
            }
        }
    }
}
