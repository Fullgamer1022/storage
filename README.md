# Storage Net (Minecraft 1.7.10 / Forge)

Mod de armazenamento original, no estilo do Simple Storage Network: **os seus baus sao o armazenamento**.
Sem discos, sem energia, sem canais.

## Blocos
- **Master**: o coracao da rede. Precisa de **exatamente UM** por rede.
- **Cabo**: liga as partes da rede (blocos encostados lado a lado se conectam).
- **Link Cable** (cabo de ligacao): coloque encostado num bau (ou outro inventario) para ele entrar na rede.
  Baus duplos funcionam.
- **Import Cable**: puxa itens do inventario encostado e manda para a rede.
- **Export Cable**: tira da rede o item do filtro e coloca no inventario encostado.
  Clique direito com um item na mao para definir o filtro. Agachado + mao vazia limpa o filtro.
- **Request Table**: a tela da rede. Mostra tudo que esta nos baus ligados e tem uma grade de crafting 3x3.

## Como montar
Master + cabos ate a Request Table + Link Cable encostado em cada bau.

### Na Request Table
- Clique esquerdo num item: pega 1 stack.  Clique direito: pega metade do stack.
- Shift + clique esquerdo: manda o stack direto para o inventario.
- Com um item no cursor, clique na lista: guarda tudo (direito: guarda 1).
- Shift + clique num item do seu inventario: guarda na rede.
- Barra de busca; botoes ^ e v (ou roda do mouse) para rolar.
- A grade 3x3 faz crafting normal. Shift + clique no resultado manda para o inventario.

## Receitas
- Cabo (x4): ferro, redstone, ferro (em linha).
- Master: ferro e redstone em volta, diamante no meio.
- Request Table: 3 paineis de vidro / ferro-cabo-ferro / 3 redstone.
- Link Cable: cabo + bau.  Import Cable: cabo + funil.  Export Cable: cabo + liberador.
(Veja o metodo init() em StorageNet.java para mudar.)

## Como compilar sem computador
1. Envie esta pasta para um repositorio no GitHub.
2. Aba **Actions** > **Build mod** (roda sozinho a cada envio).
3. Baixe o artefato **StorageNet-jar** e coloque o .jar na pasta `mods`.

## Creditos
As texturas dos blocos e da tela vem do mod Simple Storage Network (autores MrRiegel e Lothrazar).
O codigo do Storage Net e original. Se for publicar o mod, confira a licenca do Simple Storage Network
e mantenha os creditos.
