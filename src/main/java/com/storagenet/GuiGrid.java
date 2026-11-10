package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

@SideOnly(Side.CLIENT)
public class GuiGrid extends GuiContainer {
    private static final int COLS = 9;
    private static final int ROWS = 5;
    private static final int LX = 17;
    private static final int LY = 22;

    private static class Row {
        StackEntry e;
        ItemStack stack;
        String name;
    }

    private final ContainerGrid grid;
    private GuiTextField search;
    private int scroll = 0;
    private int seenVersion = -1;
    private String seenSearch = "";
    private List<Row> view = new ArrayList<Row>();

    public GuiGrid(ContainerGrid c) {
        super(c);
        this.grid = c;
        xSize = 194;
        ySize = 206;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        search = new GuiTextField(fontRendererObj, guiLeft + 88, guiTop + 6, 86, 12);
        search.setMaxStringLength(32);
        search.setFocused(false);
        buttonList.add(new GuiButton(0, guiLeft + 180, guiTop + LY, 12, 20, "^"));
        buttonList.add(new GuiButton(1, guiLeft + 180, guiTop + LY + ROWS * 18 - 20, 12, 20, "v"));
        seenVersion = -1;
        StorageNet.NET.sendToServer(new MessageGridClick(MessageGridClick.REFRESH, null));
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        search.updateCursorCounter();
    }

    private void rebuild() {
        seenVersion = grid.version;
        seenSearch = search.getText();
        String q = seenSearch.toLowerCase();
        List<Row> rows = new ArrayList<Row>();
        for (StackEntry e : grid.entries) {
            ItemStack s = e.toStack(1);
            if (s == null) continue;
            String name = s.getDisplayName();
            if (q.length() > 0 && !name.toLowerCase().contains(q)) continue;
            Row r = new Row();
            r.e = e;
            r.stack = s;
            r.name = name;
            rows.add(r);
        }
        Collections.sort(rows, new Comparator<Row>() {
            @Override
            public int compare(Row a, Row b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        view = rows;
        clampScroll();
    }

    private void clampScroll() {
        int totalRows = (view.size() + COLS - 1) / COLS;
        int max = Math.max(0, totalRows - ROWS);
        if (scroll > max) scroll = max;
        if (scroll < 0) scroll = 0;
    }

    private int indexAt(int mx, int my) {
        int rx = mx - guiLeft - LX;
        int ry = my - guiTop - LY;
        if (rx < 0 || ry < 0 || rx >= COLS * 18 || ry >= ROWS * 18) return -1;
        return (scroll + ry / 18) * COLS + rx / 18;
    }

    private boolean inList(int mx, int my) {
        int rx = mx - guiLeft - LX;
        int ry = my - guiTop - LY;
        return rx >= 0 && ry >= 0 && rx < COLS * 18 && ry < ROWS * 18;
    }

    private static String fmt(int n) {
        if (n < 1000) return String.valueOf(n);
        if (n < 1000000) return (n / 1000) + "." + ((n % 1000) / 100) + "K";
        return (n / 1000000) + "." + ((n % 1000000) / 100000) + "M";
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        if (seenVersion != grid.version || !search.getText().equals(seenSearch)) rebuild();

        GuiUtil.panel(guiLeft, guiTop, xSize, ySize);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                GuiUtil.cell(guiLeft + LX + c * 18, guiTop + LY + r * 18);
            }
        }
        for (Object o : inventorySlots.inventorySlots) {
            GuiUtil.slotCell(guiLeft, guiTop, (Slot) o);
        }

        GL11.glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int idx = (scroll + r) * COLS + c;
                if (idx >= view.size()) continue;
                Row row = view.get(idx);
                int px = guiLeft + LX + c * 18 + 1;
                int py = guiTop + LY + r * 18 + 1;
                itemRender.zLevel = 100.0F;
                itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), row.stack, px, py);
                String txt = row.e.count == 1 ? null : fmt(row.e.count);
                itemRender.renderItemOverlayIntoGUI(fontRendererObj, mc.getTextureManager(), row.stack, px, py, txt);
                itemRender.zLevel = 0.0F;
            }
        }
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();

        search.drawTextBox();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRendererObj.drawString(StatCollector.translateToLocal("tile.storagenet.request.name"), LX, 8, 0x404040);
        if (grid.status != 0) {
            fontRendererObj.drawSplitString(StatCollector.translateToLocal("gui.storagenet.status." + grid.status), LX + 4, LY + 4, 150, 0x8B0000);
        }
        int idx = indexAt(mx, my);
        if (idx >= 0 && idx < view.size()) {
            Row row = view.get(idx);
            int rel = idx - scroll * COLS;
            int cx = LX + (rel % COLS) * 18 + 1;
            int cy = LY + (rel / COLS) * 18 + 1;
            drawRect(cx, cy, cx + 16, cy + 16, 0x80FFFFFF);
            List tip = row.stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
            tip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("gui.storagenet.amount", row.e.count));
            drawHoveringText(tip, mx - guiLeft, my - guiTop, fontRendererObj);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) {
        if (inList(mx, my)) {
            search.setFocused(false);
            ItemStack cursor = mc.thePlayer.inventory.getItemStack();
            int idx = indexAt(mx, my);
            MessageGridClick m;
            if (cursor != null) {
                m = new MessageGridClick(btn == 1 ? MessageGridClick.INSERT_ONE : MessageGridClick.INSERT_ALL, null);
            } else if (idx >= 0 && idx < view.size()) {
                int mode;
                if (btn == 1) mode = MessageGridClick.TAKE_HALF;
                else if (isShiftKeyDown()) mode = MessageGridClick.TAKE_TO_INV;
                else mode = MessageGridClick.TAKE_STACK;
                m = new MessageGridClick(mode, view.get(idx).e);
            } else {
                return;
            }
            StorageNet.NET.sendToServer(m);
            return;
        }
        search.mouseClicked(mx, my, btn);
        super.mouseClicked(mx, my, btn);
    }

    @Override
    protected void keyTyped(char c, int k) {
        if (k != Keyboard.KEY_ESCAPE && search.textboxKeyTyped(c, k)) {
            scroll = 0;
            return;
        }
        super.keyTyped(c, k);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == 0) scroll--;
        if (b.id == 1) scroll++;
        clampScroll();
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) {
            scroll += d > 0 ? -1 : 1;
            clampScroll();
        }
    }
}
