package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.Gui;
import net.minecraft.inventory.Slot;

@SideOnly(Side.CLIENT)
public class GuiUtil {
    public static void panel(int x, int y, int w, int h) {
        Gui.drawRect(x, y, x + w, y + h, 0xFF000000);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        Gui.drawRect(x + 1, y + 1, x + w - 2, y + 2, 0xFFFFFFFF);
        Gui.drawRect(x + 1, y + 1, x + 2, y + h - 2, 0xFFFFFFFF);
        Gui.drawRect(x + 2, y + h - 2, x + w - 1, y + h - 1, 0xFF555555);
        Gui.drawRect(x + w - 2, y + 2, x + w - 1, y + h - 1, 0xFF555555);
    }

    /** Celula 18x18 (item de 16x16 por dentro). */
    public static void cell(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 18, 0xFF373737);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    public static void slotCell(int left, int top, Slot s) {
        cell(left + s.xDisplayPosition - 1, top + s.yDisplayPosition - 1);
    }
}
