package xyz.overdyn.dyngui.items;

import org.bukkit.Color;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import xyz.overdyn.dyngui.items.minecraft.ItemMinecraft;
import xyz.overdyn.dyngui.items.minecraft.meta.ItemData;

final class LegacyItemAdapter {

    private LegacyItemAdapter() {}

    static GuiItem convert(@NotNull ItemWrapper item) {
        ItemData data = new ItemMinecraft(
                item.material(),
                item.itemStack().getAmount()
        );

        data.setDisplayName(item.displayName());
        data.setLore(item.lore());
        data.setCustomModelData(item.customModelData());

        if (item.getEnchantments() != null) {
            item.getEnchantments()
                    .forEach(e -> data.addEnchant(e.enchantment(), e.level()));
        }

        if (item.enchanted()) {
            data.addEnchant(Enchantment.LURE, 1);
        }

        if (item.flags() != null) {
            data.addItemFlags(item.flags());
        }

        if (item.getBase64Skin() != null) {
            data.setSkullTextureBase64(item.getBase64Skin());
        }

        data.setColor(Color.fromRGB(
                item.getRed(),
                item.getGreen(),
                item.getBlue()
        ));

        return new GuiItem(data);
    }
}
