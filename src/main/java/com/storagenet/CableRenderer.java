package com.storagenet;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

/** Desenha os cabos finos: um nucleo no meio e "pinos" ate cada vizinho conectado. */
public class CableRenderer implements ISimpleBlockRenderingHandler {
    private static final double C0 = 5.0 / 16.0;
    private static final double C1 = 11.0 / 16.0;
    private static final double P0 = 6.0 / 16.0;
    private static final double P1 = 10.0 / 16.0;
    private static final double T = 1.0 / 16.0;

    @Override
    public int getRenderId() {
        return StorageNet.cableRenderId;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks rb) {
        IIcon icon = block.getIcon(0, 0);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        drawBox(rb, block, icon, C0, C0, C0, C1, C1, C1);
        drawBox(rb, block, icon, 0.0, P0, P0, C0, P1, P1);
        drawBox(rb, block, icon, C1, P0, P0, 1.0, P1, P1);
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        rb.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    }

    private void drawBox(RenderBlocks rb, Block b, IIcon icon,
                         double x0, double y0, double z0, double x1, double y1, double z1) {
        Tessellator t = Tessellator.instance;
        rb.setRenderBounds(x0, y0, z0, x1, y1, z1);
        t.startDrawingQuads();
        t.setNormal(0.0F, -1.0F, 0.0F);
        rb.renderFaceYNeg(b, 0.0D, 0.0D, 0.0D, icon);
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0.0F, 1.0F, 0.0F);
        rb.renderFaceYPos(b, 0.0D, 0.0D, 0.0D, icon);
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, -1.0F);
        rb.renderFaceZNeg(b, 0.0D, 0.0D, 0.0D, icon);
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, 1.0F);
        rb.renderFaceZPos(b, 0.0D, 0.0D, 0.0D, icon);
        t.draw();
        t.startDrawingQuads();
        t.setNormal(-1.0F, 0.0F, 0.0F);
        rb.renderFaceXNeg(b, 0.0D, 0.0D, 0.0D, icon);
        t.draw();
        t.startDrawingQuads();
        t.setNormal(1.0F, 0.0F, 0.0F);
        rb.renderFaceXPos(b, 0.0D, 0.0D, 0.0D, icon);
        t.draw();
    }

    private void box(RenderBlocks rb, Block b, IIcon override, int x, int y, int z,
                     double x0, double y0, double z0, double x1, double y1, double z1) {
        rb.setRenderBounds(x0, y0, z0, x1, y1, z1);
        if (override != null) rb.setOverrideBlockTexture(override);
        rb.renderStandardBlock(b, x, y, z);
        if (override != null) rb.clearOverrideBlockTexture();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess w, int x, int y, int z, Block block, int modelId, RenderBlocks rb) {
        BlockNode b = (BlockNode) block;
        box(rb, block, null, x, y, z, C0, C0, C0, C1, C1, C1);
        for (int s = 0; s < 6; s++) {
            boolean inv = b.connectsInv(w, x, y, z, s);
            if (!inv && !b.connectsNode(w, x, y, z, s)) continue;
            double lo = inv ? T : 0.0;
            double hi = inv ? 1.0 - T : 1.0;
            switch (s) {
                case 0:
                    box(rb, block, null, x, y, z, P0, lo, P0, P1, C0, P1);
                    if (inv) box(rb, block, b.plateIcon, x, y, z, C0, 0.0, C0, C1, T, C1);
                    break;
                case 1:
                    box(rb, block, null, x, y, z, P0, C1, P0, P1, hi, P1);
                    if (inv) box(rb, block, b.plateIcon, x, y, z, C0, 1.0 - T, C0, C1, 1.0, C1);
                    break;
                case 2:
                    box(rb, block, null, x, y, z, P0, P0, lo, P1, P1, C0);
                    if (inv) box(rb, block, b.plateIcon, x, y, z, C0, C0, 0.0, C1, C1, T);
                    break;
                case 3:
                    box(rb, block, null, x, y, z, P0, P0, C1, P1, P1, hi);
                    if (inv) box(rb, block, b.plateIcon, x, y, z, C0, C0, 1.0 - T, C1, C1, 1.0);
                    break;
                case 4:
                    box(rb, block, null, x, y, z, lo, P0, P0, C0, P1, P1);
                    if (inv) box(rb, block, b.plateIcon, x, y, z, 0.0, C0, C0, T, C1, C1);
                    break;
                default:
                    box(rb, block, null, x, y, z, C1, P0, P0, hi, P1, P1);
                    if (inv) box(rb, block, b.plateIcon, x, y, z, 1.0 - T, C0, C0, 1.0, C1, C1);
                    break;
            }
        }
        rb.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        return true;
    }
}
