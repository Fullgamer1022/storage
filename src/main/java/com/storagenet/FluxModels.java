package com.storagenet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelos 3D dos blocos Flux (geometria e UVs copiados dos modelos originais do Flux Networks).
 * Cada quad: {textura, tint, (x,y,z,u,v) x4}.
 */
public class FluxModels {
    private static Map<String, float[][]> models;

    public static synchronized float[][] get(String name) {
        if (models == null) load();
        float[][] m = models.get(name);
        return m == null ? new float[0][] : m;
    }

    private static void load() {
        models = new HashMap<String, float[][]>();
        try {
            InputStream in = FluxModels.class.getResourceAsStream("/assets/storagenet/models/flux_models.txt");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            String cur = null;
            List<float[]> quads = new ArrayList<float[]>();
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                if (line.startsWith("MODEL ")) {
                    if (cur != null) models.put(cur, quads.toArray(new float[quads.size()][]));
                    cur = line.substring(6);
                    quads = new ArrayList<float[]>();
                } else {
                    String[] p = line.split(" ");
                    float[] q = new float[p.length];
                    for (int i = 0; i < p.length; i++) q[i] = Float.parseFloat(p[i]);
                    quads.add(q);
                }
            }
            if (cur != null) models.put(cur, quads.toArray(new float[quads.size()][]));
            r.close();
        } catch (Exception e) {
            throw new RuntimeException("Nao consegui ler flux_models.txt", e);
        }
    }
}
