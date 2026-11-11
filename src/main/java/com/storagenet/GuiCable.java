package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Tela de configuracao dos cabos (filtros, lista branca/negra, prioridade, direcao, upgrades, operacao). */
@SideOnly(Side.CLIENT)
public class GuiCable extends GuiContainer {
    private static final ResourceLocation TEX = new ResourceLocation(StorageNet.MODID, "textures/gui/cable.png");
    private static final int B_MINUS = 0;
    private static final int B_PLUS = 1;
    private static final int B_WHITE = 3;
    private static final int B_ACTI = 4;
    private static final int B_IMPORT = 5;
    private static final int B_WAY = 6;
    private static final int FX = 7;   // origem dos slots de filtro (fundo)
    private static final int FY = 25;

    private final TileFilter tile;
    private GuiTextField limitField;
    private CableButton btnActi;

    private class CableButton extends GuiButton {
        public CableButton(int id, int x, int y, String text) {
            super(id, x, y, 16, 16, text);
        }

        @Override
        public void drawButton(Minecraft mc, int mx, int my) {
            if (!visible) return;
            boolean hover = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(TEX);
            drawTexturedModalRect(xPosition, yPosition, 160 + 16 * (hover ? 2 : 1), 52, 16, 16);
            switch (id) {
                case B_WHITE:
                    drawTexturedModalRect(xPosition + 1, yPosition + 3, tile.white ? 176 : 190, 83, 13, 10);
                    break;
                case B_ACTI:
                    drawTexturedModalRect(xPosition, yPosition, tile.mode ? 176 : 192, 94, 16, 15);
                    break;
                case B_WAY:
                    drawTexturedModalRect(xPosition + 2, yPosition + 2, 176 + 12 * tile.way.ordinal(), 114, 12, 12);
                    break;
                default:
                    drawCenteredString(mc.fontRenderer, displayString, xPosition + 8, yPosition + 4, 0xE0E0E0);
                    break;
            }
        }
    }

    public GuiCable(ContainerCable c) {
        super(c);
        this.tile = c.tile;
        xSize = 176;
        ySize = 170;
    }

    private boolean opVisible() {
        return tile.isUpgradeable() && tile.upgradeCount(ItemUpgrade.OP) >= 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        if (tile.isStorage()) {
            buttonList.add(new CableButton(B_MINUS, guiLeft + 6, guiTop + 5, "-"));
            buttonList.add(new CableButton(B_PLUS, guiLeft + 37, guiTop + 5, "+"));
            buttonList.add(new CableButton(B_WAY, guiLeft + 115, guiTop + 5, ""));
        }
        buttonList.add(new CableButton(B_WHITE, guiLeft + 58, guiTop + 5, ""));
        buttonList.add(new CableButton(B_IMPORT, guiLeft + 78, guiTop + 5, "I"));
        btnActi = new CableButton(B_ACTI, guiLeft + 127, guiTop + 65, "");
        buttonList.add(btnActi);
        limitField = new GuiTextField(fontRendererObj, guiLeft + 34, guiTop + 69, 85, fontRendererObj.FONT_HEIGHT);
        limitField.setMaxStringLength(9);
        limitField.setEnableBackgroundDrawing(false);
        limitField.setFocused(false);
        limitField.setText(String.valueOf(tile.limit));
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        limitField.updateCursorCounter();
        btnActi.visible = opVisible();
        if (!limitField.isFocused() && !limitField.getText().equals(String.valueOf(tile.limit))) {
            limitField.setText(String.valueOf(tile.limit));
        }
    }

    private void send(int action, int a, int b) {
        StorageNet.NET.sendToServer(new MessageCable(action, tile, a, b));
    }

    private int filterAt(int mx, int my) {
        int rx = mx - guiLeft - FX;
        int ry = my - guiTop - FY;
        if (rx < 0 || ry < 0 || rx >= 9 * 18 || ry >= 2 * 18) return -1;
        return (ry / 18) * 9 + rx / 18;
    }

    private boolean inOpSlot(int mx, int my) {
        int rx = mx - guiLeft - 7;
        int ry = my - guiTop - 65;
        return rx >= 0 && ry >= 0 && rx < 18 && ry < 18;
    }

    private void drawItem(ItemStack s, int x, int y, String label) {
        itemRender.zLevel = 100.0F;
        itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), s, x, y);
        itemRender.renderItemOverlayIntoGUI(fontRendererObj, mc.getTextureManager(), s, x, y, label);
        itemRender.zLevel = 0.0F;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEX);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 9; c++) {
                drawTexturedModalRect(guiLeft + FX + c * 18, guiTop + FY + r * 18, 176, 34, 18, 18);
            }
        }
        if (tile.isUpgradeable()) {
            for (int i = 0; i < 4; i++) drawTexturedModalRect(guiLeft + 97 + i * 18, guiTop + 5, 176, 34, 18, 18);
        }
        boolean op = opVisible();
        if (op) {
            drawTexturedModalRect(guiLeft + 7, guiTop + 65, 176, 34, 18, 18);
            drawTexturedModalRect(guiLeft + 30, guiTop + 67, 0, 171, 90, 12);
        }

        GL11.glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        boolean stock = tile.isUpgradeable() && tile.upgradeCount(ItemUpgrade.STOCK) > 0;
        for (int i = 0; i < TileFilter.SLOTS; i++) {
            ItemStack f = tile.filter[i];
            if (f == null) continue;
            int px = guiLeft + FX + (i % 9) * 18 + 1;
            int py = guiTop + FY + (i / 9) * 18 + 1;
            drawItem(f, px, py, stock ? String.valueOf(f.stackSize) : "");
        }
        if (op && tile.opStack != null) drawItem(tile.opStack, guiLeft + 8, guiTop + 66, "");
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();

        if (op) limitField.drawTextBox();
    }

    private void smallText(String t, int x, int y, int color) {
        GL11.glPushMatrix();
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        fontRendererObj.drawStringWithShadow(t, x * 2, y * 2, color);
        GL11.glPopMatrix();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        if (tile.isStorage()) {
            String p = String.valueOf(tile.priority);
            fontRendererObj.drawString(p, 30 - fontRendererObj.getStringWidth(p) / 2, 10, 0x404040);
        }
        // letras M (dano) e O (OreDict) nos filtros marcados
        for (int i = 0; i < TileFilter.SLOTS; i++) {
            if (tile.filter[i] == null) continue;
            int px = FX + (i % 9) * 18 + 2;
            int py = FY + (i / 9) * 18 + 2;
            if (tile.meta[i]) smallText("M", px, py, 0x55FFFF);
            if (tile.ore[i]) smallText("O", px + 10, py, 0xFFAA00);
        }
        int rx = mx - guiLeft;
        int ry = my - guiTop;
        List tip = null;
        int fi = filterAt(mx, my);
        if (fi >= 0) {
            int cx = FX + (fi % 9) * 18 + 1;
            int cy = FY + (fi / 9) * 18 + 1;
            drawRect(cx, cy, cx + 16, cy + 16, 0x80FFFFFF);
            ItemStack f = tile.filter[fi];
            if (f != null) {
                tip = f.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
                tip.add(EnumChatFormatting.AQUA + "M: " + StatCollector.translateToLocal(
                        tile.meta[fi] ? "gui.storagenet.on" : "gui.storagenet.off") + " - "
                        + StatCollector.translateToLocal("gui.storagenet.hint.meta"));
                tip.add(EnumChatFormatting.GOLD + "O: " + StatCollector.translateToLocal(
                        tile.ore[fi] ? "gui.storagenet.on" : "gui.storagenet.off") + " - "
                        + StatCollector.translateToLocal("gui.storagenet.hint.ore"));
                tip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("gui.storagenet.hint.clear"));
            } else {
                tip = new ArrayList();
                tip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("gui.storagenet.hint.ghost"));
            }
        } else if (opVisible() && inOpSlot(mx, my)) {
            tip = new ArrayList();
            tip.add(StatCollector.translateToLocal("gui.storagenet.hint.op"));
        } else {
            for (Object o : buttonList) {
                GuiButton b = (GuiButton) o;
                if (!b.visible || mx < b.xPosition || my < b.yPosition || mx >= b.xPosition + b.width
                        || my >= b.yPosition + b.height) continue;
                tip = new ArrayList();
                switch (b.id) {
                    case B_MINUS:
                    case B_PLUS:
                        tip.add(StatCollector.translateToLocal("gui.storagenet.priority"));
                        break;
                    case B_WHITE:
                        tip.add(StatCollector.translateToLocal(tile.white ? "gui.storagenet.whitelist" : "gui.storagenet.blacklist"));
                        break;
                    case B_IMPORT:
                        tip.add(StatCollector.translateToLocal("gui.storagenet.importfilter"));
                        break;
                    case B_WAY:
                        tip.add(StatCollector.translateToLocal("gui.storagenet.way." + tile.way.name()));
                        break;
                    case B_ACTI:
                        tip.add(StatCollector.translateToLocalFormatted("gui.storagenet.operate",
                                tile.opStack == null ? "-" : tile.opStack.getDisplayName(),
                                StatCollector.translateToLocal(tile.mode ? "gui.storagenet.more" : "gui.storagenet.less"),
                                tile.limit));
                        break;
                    default:
                        tip = null;
                        break;
                }
                break;
            }
        }
        if (tip != null) drawHoveringText(tip, rx, ry, fontRendererObj);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) {
        int fi = filterAt(mx, my);
        if (fi >= 0) {
            send(MessageCable.GHOST, fi, (btn == 1 ? 1 : 0) | (isShiftKeyDown() ? 2 : 0));
            return;
        }
        if (opVisible()) {
            if (inOpSlot(mx, my)) {
                send(MessageCable.OP_ITEM, 0, 0);
                return;
            }
            limitField.mouseClicked(mx, my, btn);
        }
        super.mouseClicked(mx, my, btn);
    }

    @Override
    protected void keyTyped(char c, int k) {
        if (opVisible() && limitField.isFocused()) {
            if (k == Keyboard.KEY_ESCAPE) {
                super.keyTyped(c, k);
                return;
            }
            if (Character.isDigit(c) || c < 32 || c == 127) {
                limitField.textboxKeyTyped(c, k);
                String t = limitField.getText().replaceAll("[^0-9]", "");
                if (!t.equals(limitField.getText())) limitField.setText(t);
                int v = 0;
                try {
                    v = t.length() == 0 ? 0 : Integer.parseInt(t);
                } catch (NumberFormatException e) {
                    v = 0;
                }
                send(MessageCable.LIMIT, v, 0);
            }
            return;
        }
        super.keyTyped(c, k);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case B_MINUS:
                send(MessageCable.PRIORITY, -1, 0);
                break;
            case B_PLUS:
                send(MessageCable.PRIORITY, 1, 0);
                break;
            case B_WHITE:
                send(MessageCable.WHITE, 0, 0);
                break;
            case B_ACTI:
                send(MessageCable.MODE, 0, 0);
                break;
            case B_IMPORT:
                send(MessageCable.IMPORT_FILTER, 0, 0);
                break;
            case B_WAY:
                send(MessageCable.WAY, 0, 0);
                break;
            default:
                break;
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int fi = filterAt(mx, my);
        if (fi >= 0 && tile.filter[fi] != null) {
            send(MessageCable.ADJUST, fi, (d > 0 ? 1 : -1) * (isShiftKeyDown() ? 10 : 1));
        }
    }
}
