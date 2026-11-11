package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

@SideOnly(Side.CLIENT)
public class GuiGrid extends GuiContainer {
    private static final ResourceLocation TEX = new ResourceLocation(StorageNet.MODID, "textures/gui/request.png");
    private static final int COLS = 9;
    private static final int ROWS = 4;
    private static final int LX = 8;
    private static final int LY = 8;

    private static class Row {
        StackEntry e;
        ItemStack stack;
        String name;
        String mod;
    }

    private class GridButton extends GuiButton {
        public GridButton(int id, int x, int y, String text) {
            super(id, x, y, 14, 14, text);
        }

        @Override
        public void drawButton(Minecraft mc, int mx, int my) {
            if (!visible) return;
            boolean hover = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(TEX);
            drawTexturedModalRect(xPosition, yPosition, 162 + 14 * (hover ? 2 : 1), 0, 14, 14);
            drawCenteredString(mc.fontRenderer, displayString, xPosition + 7, yPosition + 3, 0xE0E0E0);
        }
    }

    private final ContainerGrid grid;
    private GuiTextField search;
    private int scroll = 0;
    private int seenVersion = -1;
    private String seenSearch = "";
    private int seenSort = -1;
    private boolean seenDown = false;
    private GridButton btnDir;
    private GridButton btnSort;
    private List<Row> view = new ArrayList<Row>();

    public GuiGrid(ContainerGrid c) {
        super(c);
        this.grid = c;
        xSize = 176;
        ySize = 256;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        search = new GuiTextField(fontRendererObj, guiLeft + 81, guiTop + 96, 85, 10);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(30);
        search.setFocused(false);
        btnDir = new GridButton(0, guiLeft + 7, guiTop + 93, "");
        btnSort = new GridButton(1, guiLeft + 21, guiTop + 93, "");
        buttonList.add(btnDir);
        buttonList.add(btnSort);
        buttonList.add(new GridButton(2, guiLeft + 35, guiTop + 93, "^"));
        buttonList.add(new GridButton(3, guiLeft + 49, guiTop + 93, "v"));
        buttonList.add(new GridButton(5, guiLeft + 64, guiTop + 93, "X"));
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

    private static String modId(Item it) {
        String n = Item.itemRegistry.getNameForObject(it);
        if (n == null) return "";
        int i = n.indexOf(':');
        return i < 0 ? "minecraft" : n.substring(0, i);
    }

    private static String modName(String id) {
        ModContainer mc = Loader.instance().getIndexedModList().get(id);
        return mc == null ? id : mc.getName();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean matches(String q, Row r) {
        if (q.length() == 0) return true;
        char c = q.charAt(0);
        String rest = q.substring(1);
        if (c == '@') return r.mod.toLowerCase().contains(rest) || modName(r.mod).toLowerCase().contains(rest);
        if (c == '#') {
            List tip = r.stack.getTooltip(mc.thePlayer, false);
            for (int i = 1; i < tip.size(); i++) {
                if (String.valueOf(tip.get(i)).toLowerCase().contains(rest)) return true;
            }
            return false;
        }
        if (c == '$') {
            for (int id : OreDictionary.getOreIDs(r.stack)) {
                if (OreDictionary.getOreName(id).toLowerCase().contains(rest)) return true;
            }
            return false;
        }
        return r.name.toLowerCase().contains(q);
    }

    private void rebuild() {
        seenVersion = grid.version;
        seenSearch = search.getText();
        seenSort = grid.sort;
        seenDown = grid.down;
        String q = seenSearch.toLowerCase();
        List<Row> rows = new ArrayList<Row>();
        for (StackEntry e : grid.entries) {
            ItemStack s = e.toStack(1);
            if (s == null) continue;
            Row r = new Row();
            r.e = e;
            r.stack = s;
            r.name = s.getDisplayName();
            r.mod = modId(s.getItem());
            if (!matches(q, r)) continue;
            rows.add(r);
        }
        final int sortType = grid.sort;
        final boolean desc = grid.down;
        Collections.sort(rows, new Comparator<Row>() {
            @Override
            public int compare(Row a, Row b) {
                int v;
                if (sortType == 2) {
                    v = a.e.count == b.e.count ? a.name.compareToIgnoreCase(b.name) : (a.e.count < b.e.count ? -1 : 1);
                } else if (sortType == 1) {
                    v = a.mod.compareToIgnoreCase(b.mod);
                    if (v == 0) v = a.name.compareToIgnoreCase(b.name);
                } else {
                    v = a.name.compareToIgnoreCase(b.name);
                }
                return desc ? -v : v;
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

    private boolean inList(int mx, int my) {
        int rx = mx - guiLeft - LX;
        int ry = my - guiTop - LY;
        return rx >= 0 && ry >= 0 && rx < COLS * 18 && ry < ROWS * 18;
    }

    private int indexAt(int mx, int my) {
        if (!inList(mx, my)) return -1;
        int rx = mx - guiLeft - LX;
        int ry = my - guiTop - LY;
        return (scroll + ry / 18) * COLS + rx / 18;
    }

    private static String fmt(int n) {
        if (n < 1000) return String.valueOf(n);
        if (n < 1000000) return (n / 1000) + "." + ((n % 1000) / 100) + "K";
        return (n / 1000000) + "." + ((n % 1000000) / 100000) + "M";
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        if (seenVersion != grid.version || !search.getText().equals(seenSearch) || seenSort != grid.sort || seenDown != grid.down) rebuild();
        btnDir.displayString = grid.down ? "<" : ">";
        btnSort.displayString = grid.sort == 1 ? "M" : (grid.sort == 2 ? "#" : "N");

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEX);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

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
        if (grid.status != 0) {
            fontRendererObj.drawSplitString(StatCollector.translateToLocal("gui.storagenet.status." + grid.status),
                    LX + 4, LY + 4, 150, 0xFF8080);
        }
        if (mx >= btnDir.xPosition && mx < btnSort.xPosition + 14 && my >= btnDir.yPosition && my < btnDir.yPosition + 14) {
            List t = new ArrayList();
            if (mx < btnSort.xPosition) {
                t.add(StatCollector.translateToLocal(grid.down ? "gui.storagenet.dir.desc" : "gui.storagenet.dir.asc"));
            } else {
                t.add(StatCollector.translateToLocal("gui.storagenet.sort." + grid.sort));
            }
            drawHoveringText(t, mx - guiLeft, my - guiTop, fontRendererObj);
            return;
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
        int hx = mx - guiLeft;
        int hy = my - guiTop;
        if (hx >= 62 && hx < 70 && hy >= 110 && hy < 118) {
            StorageNet.NET.sendToServer(new MessageGridClick(MessageGridClick.CLEAR_GRID, null));
            return;
        }
        if (btn == 1 && hx >= 81 && hx < 169 && hy >= 94 && hy < 106) {
            search.setText("");
            search.setFocused(true);
            return;
        }
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
        switch (b.id) {
            case 0:
                StorageNet.NET.sendToServer(new MessageGridClick(MessageGridClick.DIR_TOGGLE, null));
                break;
            case 1:
                StorageNet.NET.sendToServer(new MessageGridClick(MessageGridClick.SORT_NEXT, null));
                break;
            case 2:
                scroll--;
                break;
            case 3:
                scroll++;
                break;
            case 5:
                search.setText("");
                break;
            default:
                break;
        }
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
