# Storage Net (Minecraft 1.7.10 / Forge)

Mod de armazenamento original, inspirado na ideia de "rede de armazenamento" do Refined Storage,
mas feito do zero e bem mais simples que o AE2: **sem energia, sem canais, sem crafting automatico**.

## Como usar no jogo
1. **Disk Drive**: coloque discos dentro (8 espacos).
2. **Storage Grid**: a tela da rede. Mostra tudo que esta nos discos.
3. **Storage Cable**: liga os blocos. Drive e Grid tambem se conectam encostados um no outro.
   Tudo que estiver ligado lado a lado forma uma rede.
4. **Discos**: 1K, 4K, 16K e 64K itens no total (qualquer tipo de item).

### Na Grid
- Clique esquerdo num item: pega 1 stack.  Clique direito: pega metade do stack.
- Shift + clique esquerdo: manda o stack direto para o inventario.
- Com um item no cursor, clique em qualquer lugar da lista: guarda tudo (direito: guarda 1).
- Shift + clique num item do seu inventario: guarda na rede.
- Barra de busca no topo; botoes ^ e v (ou roda do mouse) para rolar.

## Receitas
- Cabo (x4): ferro, redstone, ferro (em linha).
- Disk Drive: ferro em cima e embaixo, redstone-cabo-redstone no meio.
- Grid: 3 paineis de vidro / ferro-cabo-ferro / 3 redstone.
- Disco 1K: ferro + redstone + painel de vidro.
- Disco 4K: ouro.  16K: diamante.  64K: esmeralda + bloco de redstone.
(Veja o metodo init() em StorageNet.java para mudar.)

## Como compilar sem computador
1. Crie um repositorio no GitHub e envie todos os arquivos desta pasta.
2. Abra a aba **Actions**, rode **Build mod** (ou ele roda sozinho a cada envio).
3. Quando terminar, baixe o artefato **StorageNet-jar** e coloque o .jar na pasta `mods` do Minecraft 1.7.10 com Forge.

Se o build falhar, copie o log de erro e mande para mim.
