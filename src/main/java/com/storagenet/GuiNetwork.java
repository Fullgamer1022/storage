package com.storagenet;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;

/** Tela do Flux Configurator: nome e senha da rede, botao Criar / Entrar. */
public class GuiNetwork extends GuiContainer {
    private GuiTextField name;
    private GuiTextField pass;

    public GuiNetwork(ContainerNetwork c) {
        super(c);
        xSize = 176;
        ySize = 112;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        name = new GuiTextField(fontRendererObj, guiLeft + 12, guiTop + 38, 152, 14);
        name.setMaxStringLength(FluxNetworks.MAX_NAME);
        name.setFocused(true);
        pass = new GuiTextField(fontRendererObj, guiLeft + 12, guiTop + 70, 152, 14);
        pass.setMaxStringLength(FluxNetworks.MAX_NAME);
        buttonList.clear();
        buttonList.add(new GuiButton(0, guiLeft + 38, guiTop + 88, 100, 20,
                StatCollector.translateToLocal("gui.storagenet.net.join")));
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        name.updateCursorCounter();
        pass.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == 0) send();
    }

    private void send() {
        String n = name.getText().trim();
        if (n.length() == 0) return;
        StorageNet.NET.sendToServer(new MessageNetwork(n, pass.getText()));
        mc.thePlayer.closeScreen();
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            mc.thePlayer.closeScreen();
            return;
        }
        if (key == Keyboard.KEY_TAB) {
            boolean n = name.isFocused();
            name.setFocused(!n);
            pass.setFocused(n);
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            send();
            return;
        }
        name.textboxKeyTyped(c, key);
        pass.textboxKeyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        name.mouseClicked(x, y, button);
        pass.mouseClicked(x, y, button);
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        super.drawScreen(mx, my, partial);
        name.drawTextBox();
        pass.drawTextBox();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mx, int my) {
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF000000);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFFC6C6C6);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRendererObj.drawString(StatCollector.translateToLocal("gui.storagenet.net.title"), 12, 8, 0x404040);
        String cur = ItemFluxConfigurator.netName(mc.thePlayer.getHeldItem());
        if (cur.length() == 0) cur = StatCollector.translateToLocal("gui.storagenet.net.none");
        fontRendererObj.drawString(StatCollector.translateToLocalFormatted("gui.storagenet.net.current", cur), 12, 20,
                0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("gui.storagenet.net.name"), 12, 29, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("gui.storagenet.net.pass"), 12, 61, 0x404040);
    }
}
