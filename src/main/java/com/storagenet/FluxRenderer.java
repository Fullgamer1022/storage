package com.storagenet;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

/** Desenha os blocos Flux a partir dos modelos originais (nucleo, bracos de conexao e camada colorida). */
public class FluxRenderer implements ISimpleBlockRenderingHandler {

    @Override
    public int getRenderId() {
        return StorageNet.fluxRenderId;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    private static String coreName(BlockFlux b) {
        return b.kind == BlockFlux.PLUG ? "PLUG" : (b.kind == BlockFlux.POINT ? "POINT" : "STORAGE");
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess w, int x, int y, int z, Block block, int modelId, RenderBlocks rb) {
        BlockFlux b = (BlockFlux) block;
        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(w, x, y, z));
        int color = b.tintColor(w, x, y, z);

        boolean connected = false;
        for (int s = 0; s < 6; s++) {
            if (b.hasArm(w, x, y, z, s)) connected = true;
        }
        IIcon main = (b.kind == BlockFlux.STORAGE || connected) ? b.icons[0] : b.icons[1];

        draw(FluxModels.get(coreName(b)), b.icons, main, x, y, z, color, false);
        for (int s = 0; s < 6; s++) {
            if (b.hasArm(w, x, y, z, s)) draw(FluxModels.get("ARM_" + s), b.icons, main, x, y, z, color, false);
        }
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks rb) {
        BlockFlux b = (BlockFlux) block;
        Tessellator t = Tessellator.instance;
        int color = BlockFlux.COLORS[12];
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        t.startDrawingQuads();
        draw(FluxModels.get(coreName(b)), b.icons, b.icons[0], 0, 0, 0, color, true);
        if (b.kind != BlockFlux.STORAGE) {
            for (int s = 0; s < 6; s++) {
                draw(FluxModels.get("ARM_" + s), b.icons, b.icons[0], 0, 0, 0, color, true);
            }
        }
        t.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    private void draw(float[][] quads, IIcon[] icons, IIcon main, double x, double y, double z, int color, boolean inv) {
        Tessellator t = Tessellator.instance;
        float cr = ((color >> 16) & 255) / 255.0F;
        float cg = ((color >> 8) & 255) / 255.0F;
        float cb = (color & 255) / 255.0F;
        for (float[] q : quads) {
            int tex = (int) q[0];
            IIcon icon = tex == 0 ? main : icons[tex];
            if (icon == null) continue;

            // normal da face (vertices em sentido anti-horario visto de fora)
            float ax = q[7] - q[2], ay = q[8] - q[3], az = q[9] - q[4];
            float bx = q[12] - q[7], by = q[13] - q[8], bz = q[14] - q[9];
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len == 0.0F) continue;
            nx /= len;
            ny /= len;
            nz /= len;

            float shade;
            if (inv) {
                shade = 1.0F;
                t.setNormal(nx, ny, nz);
            } else if (Math.abs(ny) >= Math.abs(nx) && Math.abs(ny) >= Math.abs(nz)) {
                shade = ny > 0 ? 1.0F : 0.5F;
            } else if (Math.abs(nz) >= Math.abs(nx)) {
                shade = 0.8F;
            } else {
                shade = 0.6F;
            }

            if (q[1] > 0.5F) {
                t.setColorOpaque_F(cr * shade, cg * shade, cb * shade);
            } else {
                t.setColorOpaque_F(shade, shade, shade);
            }
            for (int v = 0; v < 4; v++) {
                int o = 2 + v * 5;
                t.addVertexWithUV(x + q[o], y + q[o + 1], z + q[o + 2],
                        icon.getInterpolatedU(q[o + 3]), icon.getInterpolatedV(q[o + 4]));
            }
        }
    }
}
