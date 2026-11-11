package com.storagenet;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

/** Item dos blocos da rede: so existe para mostrar a descricao (tooltip) de cada um. */
public class ItemBlockNode extends ItemBlock {
    private final Block node;

    public ItemBlockNode(Block block) {
        super(block);
        this.node = block;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack s, EntityPlayer p, List list, boolean adv) {
        if (node instanceof BlockNode) {
            String key = "tooltip.storagenet." + ((BlockNode) node).getName();
            if (StatCollector.canTranslate(key)) {
                list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(key));
            }
        }
    }
}
