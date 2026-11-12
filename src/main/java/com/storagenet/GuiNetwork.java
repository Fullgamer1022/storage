package com.storagenet;

import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.StatCollector;

/** GUI de redes Flux com abas: Redes / Criar / Config / Membros / Status. */
public class GuiNetwork extends GuiContainer {
    private static final int ROWS = 5;
    private static final int MROWS = 6;

    private int tab, scroll, row = -1, mrow = -1, color, tick, mscroll;
    private GuiTextField joinPass, fName, fPass;
    private GuiButton bJoin, bColor, bCreate, bApply, bDelete, bLeave, bPromote, bKick, bUp, bDown, bNoPass;

    public GuiNetwork(ContainerNetwork c) {
        super(c);
        xSize = 232;
        ySize = 172;
    }

    private static String t(String k) {
        return StatCollector.translateToLocal("gui.storagenet.net." + k);
    }

    private MessageFluxSync.Entry cur() {
        return FluxClient.find(FluxClient.sel);
    }

    private List<MessageFluxSync.Entry> nets() {
        return FluxClient.nets;
    }

    private void send(int type, String s1, String s2, int i1, int i2) {
        StorageNet.NET.sendToServer(new MessageFluxAction(type, s1, s2, i1, i2));
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        joinPass = new GuiTextField(fontRendererObj, guiLeft + 12, guiTop + 122, 208, 14);
        joinPass.setMaxStringLength(24);
        fName = new GuiTextField(fontRendererObj, guiLeft + 12, guiTop + 48, 208, 14);
        fName.setMaxStringLength(24);
        fPass = new GuiTextField(fontRendererObj, guiLeft + 12, guiTop + 82, 208, 14);
        fPass.setMaxStringLength(24);
        buttonList.clear();
        for (int i = 0; i < 5; i++) buttonList.add(new GuiButton(100 + i, guiLeft + 6 + i * 44, guiTop + 6, 42, 16, t("tab" + i)));
        buttonList.add(bJoin = new GuiButton(10, guiLeft + 12, guiTop + 142, 120, 20, t("join")));
        buttonList.add(bColor = new GuiButton(11, guiLeft + 12, guiTop + 104, 70, 20, t("color")));
        buttonList.add(bCreate = new GuiButton(12, guiLeft + 12, guiTop + 142, 120, 20, t("create")));
        buttonList.add(bApply = new GuiButton(13, guiLeft + 12, guiTop + 142, 70, 20, t("apply")));
        buttonList.add(bDelete = new GuiButton(14, guiLeft + 86, guiTop + 142, 70, 20, t("delete")));
        buttonList.add(bLeave = new GuiButton(15, guiLeft + 160, guiTop + 142, 60, 20, t("leave")));
        buttonList.add(bPromote = new GuiButton(16, guiLeft + 12, guiTop + 144, 100, 20, t("promote")));
        buttonList.add(bKick = new GuiButton(17, guiLeft + 116, guiTop + 144, 90, 20, t("kick")));
        buttonList.add(bUp = new GuiButton(18, guiLeft + 206, guiTop + 30, 16, 16, "^"));
        buttonList.add(bDown = new GuiButton(19, guiLeft + 206, guiTop + 104, 16, 16, "v"));
        buttonList.add(bNoPass = new GuiButton(20, guiLeft + 86, guiTop + 104, 100, 20, t("nopass")));
        request();
        refresh();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    private void request() {
        send(10, "", "", 0, 0);
    }

    private void refresh() {
        MessageFluxSync.Entry c = cur();
        int lv = c == null ? -1 : c.level;
        int sz = nets().size();
        MessageFluxSync.Entry sel = row >= 0 && row < sz ? nets().get(row) : null;
        bJoin.visible = tab == 0;
        bJoin.enabled = sel != null;
        bUp.visible = bDown.visible = tab == 0;
        bColor.visible = tab == 1 || (tab == 2 && lv >= 1);
        bCreate.visible = tab == 1;
        bApply.visible = tab == 2 && lv >= 1;
        bNoPass.visible = tab == 2 && lv >= 1;
        bDelete.visible = tab == 2 && lv == 2;
        bLeave.visible = tab == 2 && lv >= 0 && lv < 2;
        bPromote.visible = bKick.visible = tab == 3 && lv >= 1;
        if (tab == 3 && c != null) {
            boolean ok = mrow >= 0 && mrow < c.mId.size() && c.mLv.get(mrow).intValue() < 2;
            bPromote.enabled = ok && lv == 2;
            bKick.enabled = ok && (lv == 2 || c.mLv.get(mrow).intValue() == 0);
            if (ok) bPromote.displayString = c.mLv.get(mrow).intValue() == 0 ? t("promote") : t("demote");
        }
        for (int i = 0; i < 5; i++) {
            GuiButton b = (GuiButton) buttonList.get(i);
            b.enabled = i != tab;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        joinPass.updateCursorCounter();
        fName.updateCursorCounter();
        fPass.updateCursorCounter();
        if (++tick % 20 == 0) request();
        refresh();
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        MessageFluxSync.Entry c = cur();
        switch (b.id) {
            case 100: case 101: case 102: case 103: case 104:
                tab = b.id - 100;
                mrow = -1;
                joinPass.setFocused(false);
                fName.setFocused(false);
                fPass.setFocused(false);
                if (tab == 1) {
                    fName.setText("");
                    fPass.setText("");
                }
                if (tab == 2 && c != null) {
                    fName.setText(c.name);
                    fPass.setText("");
                    for (int i = 0; i < 16; i++) if ((BlockFlux.COLORS[i] & 0xFFFFFF) == (c.color & 0xFFFFFF)) color = i;
                }
                break;
            case 10:
                if (row >= 0 && row < nets().size()) {
                    MessageFluxSync.Entry e = nets().get(row);
                    send(1, "", joinPass.getText(), e.id, 0);
                    joinPass.setText("");
                }
                break;
            case 11:
                color = (color + 1) & 15;
                break;
            case 12:
                send(9, fName.getText(), fPass.getText(), 0, BlockFlux.COLORS[color]);
                tab = 0;
                break;
            case 13:
                if (c != null) {
                    if (fName.getText().trim().length() > 0 && !fName.getText().trim().equals(c.name)) send(4, fName.getText(), "", c.id, 0);
                    if (fPass.getText().length() > 0) send(5, fPass.getText(), "", c.id, 0);
                    send(6, "", "", c.id, BlockFlux.COLORS[color]);
                }
                break;
            case 14:
                if (c != null) send(3, "", "", c.id, 0);
                break;
            case 15:
                if (c != null) send(8, "", "", c.id, 0);
                break;
            case 16:
                if (c != null && mrow >= 0 && mrow < c.mId.size()) send(7, c.mId.get(mrow), "", c.id, c.mLv.get(mrow).intValue() == 0 ? 1 : 0);
                break;
            case 17:
                if (c != null && mrow >= 0 && mrow < c.mId.size()) send(7, c.mId.get(mrow), "", c.id, -1);
                break;
            case 18:
                scroll = Math.max(0, scroll - 1);
                break;
            case 19:
                scroll = Math.max(0, Math.min(nets().size() - ROWS, scroll + 1));
                break;
            case 20:
                if (c != null) send(5, "", "", c.id, 0);
                break;
            default:
                break;
        }
        refresh();
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) {
            int dir = d > 0 ? -1 : 1;
            if (tab == 0) scroll = Math.max(0, Math.min(Math.max(0, nets().size() - ROWS), scroll + dir));
            if (tab == 3) mscroll = Math.max(0, mscroll + dir);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) {
        super.mouseClicked(mx, my, btn);
        if (tab == 0) joinPass.mouseClicked(mx, my, btn);
        if (tab == 1 || tab == 2) {
            fName.mouseClicked(mx, my, btn);
            fPass.mouseClicked(mx, my, btn);
        }
        if (mx >= guiLeft + 12 && mx < guiLeft + 204) {
            if (tab == 0) {
                for (int i = 0; i < ROWS; i++) {
                    int y = guiTop + 30 + i * 18;
                    if (my >= y && my < y + 16 && scroll + i < nets().size()) row = scroll + i;
                }
            } else if (tab == 3) {
                for (int i = 0; i < MROWS; i++) {
                    int y = guiTop + 30 + i * 18;
                    if (my >= y && my < y + 16) mrow = mscroll + i;
                }
            }
        }
        refresh();
    }

    @Override
    protected void keyTyped(char c, int k) {
        if (k == 1) {
            mc.thePlayer.closeScreen();
            return;
        }
        GuiTextField[] fs = tab == 0 ? new GuiTextField[] { joinPass } : (tab == 1 || tab == 2) ? new GuiTextField[] { fName, fPass } : new GuiTextField[0];
        boolean used = false;
        for (GuiTextField f : fs) {
            if (f.isFocused()) {
                if (k == 15 && fs.length == 2) {
                    fName.setFocused(f == fPass);
                    fPass.setFocused(f == fName);
                } else if (k == 28 || k == 156) {
                    if (tab == 0) actionPerformed(bJoin);
                    else if (tab == 1) actionPerformed(bCreate);
                    else actionPerformed(bApply);
                } else {
                    f.textboxKeyTyped(c, k);
                }
                used = true;
                break;
            }
        }
        if (!used) super.keyTyped(c, k);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        super.drawScreen(mx, my, pt);
        if (tab == 0) joinPass.drawTextBox();
        if (tab == 1 || (tab == 2 && cur() != null && cur().level >= 1)) {
            fName.drawTextBox();
            fPass.drawTextBox();
        }
    }

    private void txt(String s, int x, int y, int c) {
        fontRendererObj.drawString(s, guiLeft + x, guiTop + y, c);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        drawRect(guiLeft - 1, guiTop - 1, guiLeft + xSize + 1, guiTop + ySize + 1, 0xFF2B8BDC);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF1B1F2A);
        drawRect(guiLeft + 6, guiTop + 25, guiLeft + xSize - 6, guiTop + 26, 0xFF2B8BDC);
        MessageFluxSync.Entry c = cur();
        String curName = c == null ? t("none") : c.name;
        switch (tab) {
            case 0: {
                if (nets().isEmpty()) txt(t("empty"), 14, 60, 0xFF9AA3B5);
                for (int i = 0; i < ROWS; i++) {
                    int idx = scroll + i;
                    if (idx >= nets().size()) break;
                    MessageFluxSync.Entry e = nets().get(idx);
                    int y = guiTop + 30 + i * 18;
                    drawRect(guiLeft + 12, y, guiLeft + 204, y + 16, idx == row ? 0xFF2D3A55 : 0xFF252B3A);
                    drawRect(guiLeft + 14, y + 2, guiLeft + 26, y + 14, 0xFF000000 | (e.color & 0xFFFFFF));
                    String nm = (e.locked ? "* " : "") + e.name;
                    fontRendererObj.drawString(nm, guiLeft + 30, y + 4, e.id == FluxClient.sel ? 0xFF55FF55 : 0xFFFFFFFF);
                    String ow = e.owner;
                    fontRendererObj.drawString(ow, guiLeft + 200 - fontRendererObj.getStringWidth(ow), y + 4, 0xFF9AA3B5);
                }
                txt(StatCollector.translateToLocalFormatted("gui.storagenet.net.current", new Object[] { curName }), 12, 112 - 10, 0xFFFFD84D);
                txt(t("pass"), 12, 134 - 2 - 0, 0xFF9AA3B5);
                break;
            }
            case 1:
                txt(t("name"), 12, 38, 0xFFFFFFFF);
                txt(t("pass"), 12, 72, 0xFFFFFFFF);
                txt(t("color"), 88, 110, 0xFFFFFFFF);
                drawRect(guiLeft + 140, guiTop + 106, guiLeft + 158, guiTop + 124, 0xFF000000 | (BlockFlux.COLORS[color] & 0xFFFFFF));
                break;
            case 2:
                if (c == null || c.level < 1) {
                    txt(t("noperm"), 12, 50, 0xFFFF7777);
                } else {
                    txt(t("name"), 12, 38, 0xFFFFFFFF);
                    txt(t("newpass"), 12, 72, 0xFFFFFFFF);
                    drawRect(guiLeft + 190, guiTop + 106, guiLeft + 208, guiTop + 124, 0xFF000000 | (BlockFlux.COLORS[color] & 0xFFFFFF));
                    txt(t("lv" + c.level), 12, 130, 0xFF9AA3B5);
                }
                break;
            case 3:
                if (c == null || c.level < 1) {
                    txt(t("noperm"), 12, 50, 0xFFFF7777);
                } else {
                    for (int i = 0; i < MROWS; i++) {
                        int idx = mscroll + i;
                        if (idx >= c.mId.size()) break;
                        int y = guiTop + 30 + i * 18;
                        drawRect(guiLeft + 12, y, guiLeft + 220, y + 16, idx == mrow ? 0xFF2D3A55 : 0xFF252B3A);
                        fontRendererObj.drawString(c.mName.get(idx), guiLeft + 16, y + 4, 0xFFFFFFFF);
                        String lv = t("lv" + c.mLv.get(idx).intValue());
                        fontRendererObj.drawString(lv, guiLeft + 216 - fontRendererObj.getStringWidth(lv), y + 4, 0xFF9AA3B5);
                    }
                }
                break;
            case 4:
                if (c == null) {
                    txt(t("none"), 12, 40, 0xFFFF7777);
                } else {
                    txt(c.name, 12, 34, 0xFF000000 | (c.color & 0xFFFFFF));
                    txt(StatCollector.translateToLocalFormatted("gui.storagenet.net.rate", new Object[] { Integer.valueOf(c.rate) }), 12, 52, 0xFFFFFFFF);
                    txt(StatCollector.translateToLocalFormatted("gui.storagenet.net.devices", new Object[] { Integer.valueOf(c.plugs), Integer.valueOf(c.points), Integer.valueOf(c.storages) }), 12, 66, 0xFFFFFFFF);
                    txt(StatCollector.translateToLocalFormatted("gui.storagenet.net.stored", new Object[] { Long.valueOf(c.stored) }), 12, 80, 0xFFFFFFFF);
                    txt(StatCollector.translateToLocalFormatted("gui.storagenet.net.members", new Object[] { Integer.valueOf(c.count) }), 12, 94, 0xFFFFFFFF);
                }
                break;
            default:
                break;
        }
    }
}
