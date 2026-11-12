package com.storagenet;

import java.util.ArrayList;
import java.util.List;

/** Cache do lado do cliente (sem classes client-only, seguro em servidor dedicado). */
public class FluxClient {
    public static volatile List<MessageFluxSync.Entry> nets = new ArrayList<MessageFluxSync.Entry>();
    public static volatile int sel;

    public static MessageFluxSync.Entry find(int id) {
        for (MessageFluxSync.Entry e : nets) if (e.id == id) return e;
        return null;
    }
}
