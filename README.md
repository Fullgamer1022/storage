# Storage Net (Minecraft 1.7.10 / Forge)

Mod de armazenamento original, no estilo do Simple Storage Network: **os seus baus sao o armazenamento**.
Sem discos, sem energia, sem canais.

## Blocos e itens
- **Master**: o coracao da rede. Exatamente UM por rede.
- **Cabo**: liga as partes da rede.
- **Link Cable**: encostado num bau/inventario, o coloca na rede. Clique direito abre filtros, lista branca/negra, **prioridade** (maior primeiro) e direcao (entrada/saida/ambos).
- **Import / Export Cable**: movem itens. Clique direito abre a tela com 18 filtros e 4 slots de **upgrade**.
- **Request Table**: tela da rede com busca (`@mod`, `#tooltip`, `$oredict`), ordenacao (nome/mod/quantidade, crescente/decrescente) e crafting 3x3 que guarda a grade e repoe os ingredientes pela rede. O quadradinho ao lado da grade manda a grade para a rede.
- **Upgrades** (1 de cada tipo por cabo, ate 4 por slot): Speed, Operation, Stack, Stock.
- **Storage Remote** (alcance configuravel, mesma dimensao) e **Advanced Storage Remote** (sem limite): clique direito num Master para ligar, clique no ar para abrir.

### Tela dos cabos
- Clique num filtro com um item no cursor: define o filtro (o tamanho do stack vira a quantidade do upgrade Stock).
- Mao vazia: clique = comparar dano (M), clique direito = comparar OreDict (O), shift+clique = limpar.
- Botao I: copia os itens do inventario encostado para o filtro.

## Como compilar sem computador
1. Envie esta pasta para um repositorio no GitHub.
2. Aba **Actions** > **Build mod** (roda sozinho a cada envio).
3. Baixe o artefato **StorageNet-jar** e coloque o .jar na pasta `mods`.

## Creditos
As texturas dos blocos e da tela vem do mod Simple Storage Network (autores MrRiegel e Lothrazar).
O codigo do Storage Net e original. Se for publicar o mod, confira a licenca do Simple Storage Network
e mantenha os creditos.


## Energia sem fio (Flux)
- **Flux Plug**: encoste num gerador ou bateria. Ele puxa a energia (RF) para a rede.
- **Flux Point**: encoste numa maquina. Ele entrega a energia da rede.
- **Flux Storage**: guarda a energia que sobra e devolve quando falta.
- A rede e escolhida pela **cor**: clique direito no bloco com um **corante**. Todos os blocos da mesma cor
  trocam energia, em qualquer distancia e qualquer dimensao. Clique com a mao vazia mostra a cor (e a carga do Storage).
- Funciona com mods que usam RF (Thermal Expansion, Ender IO, Mekanism, Immersive Engineering...). Nao suporta EU (IC2).
- Cada bloco move ate 1.000.000 RF por tick.
- Receitas: Plug e Point = obsidiana + redstone com ender pearl (Plug) ou olho do fim (Point) no meio;
  Storage = obsidiana + ferro + bloco de redstone.

Creditos extras: texturas do Flux Networks (BloCamLimb, Ollie Lansdell). Codigo original.
