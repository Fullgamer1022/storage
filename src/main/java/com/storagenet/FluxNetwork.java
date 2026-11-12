package com.storagenet;

/** Uma rede Flux: nome, senha (vazia = aberta) e dono. */
public class FluxNetwork {
    public final int id;
    public String name;
    public String password;
    public String owner;

    public FluxNetwork(int id, String name, String password, String owner) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.owner = owner;
    }
}
