package com.storagenet;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;

/** GUI de um bloco Flux: nome, limite de transferencia, prioridade e rede. */
public class GuiFluxTile extends GuiContainer {
    private final int tx, ty, tz;
    private GuiTextField fName, fLimit;
    private int prio, net = -1, tick;
    private boolean unl;
    private GuiButton bUnl;

    public GuiFluxTile(ContainerNetwork c, int x, int y, int z) {
        super(c);
        tx = x;
        ty = y;
        tz = z;
        xSize = 210;
        ySize = 164;
    }

    private static String t(String k) {
        return StatCollector.translateToLocal("gui.storagenet.tile." + k);
    }

    private TileFlux tile() {
        TileEntity te = mc.theWorld.getTileEntity(tx, ty, tz);
        return te instanceof TileFlux ? (TileFlux) te : null;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        TileFlux tf = tile();
        fName = new GuiTextField(fontRendererObj, guiLeft + 12, guiTop + 30, 186, 14);
        fName.setMaxStringLength(24);
        fLimit = new GuiTextField(fontRendererObj, guiLeft + 12, guiTop + 62, 90, 14);
        fLimit.setMaxStringLength(7);
        if (tf != null) {
            fName.setText(tf.customName);
            fLimit.setText(String.valueOf(tf.limit));
            prio = tf.priority;
            unl = tf.unlimited;
        }
        buttonList.clear();
        buttonList.add(bUnl = new GuiButton(3, guiLeft + 108, guiTop + 58, 90, 20, ""));
        buttonList.add(new GuiButton(1, guiLeft + 110, guiTop + 84, 40, 20, "-"));
        buttonList.add(new GuiButton(2, guiLeft + 158, guiTop + 84, 40, 20, "+"));
        buttonList.add(new GuiButton(4, guiLeft + 12, guiTop + 116, 120, 20, t("usecfg")));
        buttonList.add(new GuiButton(6, guiLeft + 136, guiTop + 116, 62, 20, t("disconnect")));
        buttonList.add(new GuiButton(5, guiLeft + 12, guiTop + 140, 186, 18, t("apply")));
        StorageNet.NET.sendToServer(new MessageFluxAction(10, "", "", 0, 0));
        updateLabels();
    }

    private void updateLabels() {
        bUnl.displayString = t("unlimited") + ": " + StatCollector.translateToLocal(unl ? "gui.storagenet.on" : "gui.storagenet.off");
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        fName.updateCursorCounter();
        fLimit.updateCursorCounter();
        if (++tick % 20 == 0) StorageNet.NET.sendToServer(new MessageFluxAction(10, "", "", 0, 0));
    }

    private int limitValue() {
        try {
            return Math.max(0, Math.min(TileFlux.LIMIT, Integer.parseInt(fLimit.getText().trim())));
        } catch (NumberFormatException e) {
            return TileFlux.LIMIT;
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        int step = GuiScreen.isShiftKeyDown() ? 10 : 1;
        switch (b.id) {
            case 1:
                prio = Math.max(-99, prio - step);
                break;
            case 2:
                prio = Math.min(99, prio + step);
                break;
            case 3:
                unl = !unl;
                break;
            case 4:
                net = FluxClient.sel;
                break;
            case 6:
                net = 0;
                break;
            case 5:
                StorageNet.NET.sendToServer(new MessageFluxTile(tx, ty, tz, fName.getText(), prio, limitValue(), unl, net));
                net = -1;
                break;
            default:
                break;
        }
        updateLabels();
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) {
        super.mouseClicked(mx, my, btn);
        fName.mouseClicked(mx, my, btn);
        fLimit.mouseClicked(mx, my, btn);
    }

    @Override
    protected void keyTyped(char c, int k) {
        if (k == 1) {
            mc.thePlayer.closeScreen();
            return;
        }
        if (fName.isFocused()) {
            fName.textboxKeyTyped(c, k);
        } else if (fLimit.isFocused()) {
            if (Character.isDigit(c) || k == 14 || k == 203 || k == 205 || k == 211 || k == 199 || k == 207) fLimit.textboxKeyTyped(c, k);
        } else {
            super.keyTyped(c, k);
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        super.drawScreen(mx, my, pt);
        fName.drawTextBox();
        fLimit.drawTextBox();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        drawRect(guiLeft - 1, guiTop - 1, guiLeft + xSize + 1, guiTop + ySize + 1, 0xFF2B8BDC);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF1B1F2A);
        TileFlux tf = tile();
        String title = tf == null ? "Flux" : tf.getBlockType().getLocalizedName();
        fontRendererObj.drawString(title, guiLeft + 12, guiTop + 7, 0xFFFFFFFF);
        fontRendererObj.drawString(t("name"), guiLeft + 12, guiTop + 20, 0xFF9AA3B5);
        fontRendererObj.drawString(t("limit"), guiLeft + 12, guiTop + 52, 0xFF9AA3B5);
        fontRendererObj.drawString(t("priority") + ": " + prio, guiLeft + 12, guiTop + 90, 0xFFFFFFFF);
        String netName;
        if (net == 0) {
            netName = t("none") + " *";
        } else if (net > 0) {
            MessageFluxSync.Entry e = FluxClient.find(net);
            netName = (e == null ? "#" + net : e.name) + " *";
        } else if (tf != null && tf.network > 0) {
            MessageFluxSync.Entry e = FluxClient.find(tf.network);
            netName = e == null ? "#" + tf.network : e.name;
        } else {
            netName = t("none");
        }
        fontRendererObj.drawString(t("network") + ": " + netName, guiLeft + 12, guiTop + 106, 0xFFFFD84D);
    }
}
