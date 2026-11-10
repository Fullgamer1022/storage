package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StatCollector;

@SideOnly(Side.CLIENT)
public class GuiDrive extends GuiContainer {
    public GuiDrive(ContainerDrive c) {
        super(c);
        xSize = 176;
        ySize = 166;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        GuiUtil.panel(guiLeft, guiTop, xSize, ySize);
        for (Object o : inventorySlots.inventorySlots) {
            GuiUtil.slotCell(guiLeft, guiTop, (Slot) o);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRendererObj.drawString(StatCollector.translateToLocal("tile.storagenet.drive.name"), 8, 6, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, 72, 0x404040);
    }
}
