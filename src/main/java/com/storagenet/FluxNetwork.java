package com.storagenet;

import java.util.LinkedHashMap;
import java.util.Map;

public class FluxNetwork {
    public final int id;
    public String name;
    public String password;
    public String owner;
    public String ownerName = "";
    public int color = 0x2B8BDC;
    /** uuid -> nivel (0 = membro, 1 = admin). O dono (owner) e nivel 2. */
    public final Map<String, Integer> members = new LinkedHashMap<String, Integer>();
    public final Map<String, String> memberNames = new LinkedHashMap<String, String>();
    // estatisticas (calculadas pelo FluxManager)
    public int rate, plugs, points, storages;
    public long stored;

    public FluxNetwork(int id, String name, String password, String owner) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.owner = owner;
    }

    public int level(String uuid) {
        if (uuid == null) return -1;
        if (uuid.equals(owner)) return 2;
        Integer l = members.get(uuid);
        return l == null ? -1 : l.intValue();
    }
}
