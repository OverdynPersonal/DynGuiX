package xyz.overdyn.dyngui.form.v2;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import xyz.overdyn.dyngui.abstracts.AbstractGuiLayer;
import xyz.overdyn.dyngui.policy.GuiPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiBuilder extends AbstractGuiLayer {
    /**
     * Constructs a GUI layer with a custom inventory size.
     *
     * @param size   Number of slots in the inventory (must be multiple of 9)
     * @param title  The display title of the GUI
     * @param policy The GUI policy defining interaction rules
     */
    public GuiBuilder(int size, @NotNull Component title, GuiPolicy policy) {
        super(size, title, policy);
    }

    /**
     * Constructs a GUI layer with a specific inventory type.
     *
     * @param type   The type of inventory to use (e.g., CHEST, DISPENSER)
     * @param title  The display title of the GUI
     * @param policy The GUI policy defining interaction rules
     */
    public GuiBuilder(InventoryType type, @NotNull Component title, GuiPolicy policy) {
        super(type, title, policy);
    }


    public static GuiBuilder loadFrom(@NotNull InventoryUI inventoryUI) {
        // Пробегаем все кнопки\
        var type = inventoryUI.meta(InventoryUI.MenuProperty.TYPE);
        GuiBuilder gui;
        if (type != null) {
            gui = new GuiBuilder(
                type,
                LegacyComponentSerializer.legacyAmpersand().deserialize(inventoryUI.meta(InventoryUI.MenuProperty.TITLE)),
                GuiPolicy.Factories.HIGHEST
            );
        } else {
            var size = inventoryUI.meta(InventoryUI.MenuProperty.SIZE);
            gui = new GuiBuilder(
                    size == -1 ? 54 : size,
                    LegacyComponentSerializer.legacyAmpersand().deserialize(inventoryUI.meta(InventoryUI.MenuProperty.TITLE)),
                    GuiPolicy.Factories.HIGHEST
            );
        }

        Map<Integer, List<InventoryUI.Button>> slotMap = new HashMap<>();

        for (InventoryUI.Button button : inventoryUI.meta(InventoryUI.MenuProperty.BUTTONS).values()) {
            int slot = button.meta(InventoryUI.Button.ButtonProperty.SLOT);
            slotMap.computeIfAbsent(slot, k -> new ArrayList<>()).add(button);
        }


        for (Map.Entry<Integer, List<InventoryUI.Button>> entry : slotMap.entrySet()) {
            List<InventoryUI.Button> candidates = entry.getValue();

            candidates.sort((b1, b2) -> Integer.compare(
                    b2.meta(InventoryUI.Button.ButtonProperty.PRIORITY),
                    b1.meta(InventoryUI.Button.ButtonProperty.PRIORITY)
            ));

            InventoryUI.Button selected = null;
            for (InventoryUI.Button b : candidates) {
                var requirements = b.meta(InventoryUI.Button.ButtonProperty.VIEW_REQUIREMENTS);
//                if (requirements == null || requirements.stream().allMatch(InventoryUI.Requirement::test)) {
//                    selected = b;
//                    break;
//                }
            }

            if (selected != null) {
                gui.registerItem(selected.meta(InventoryUI.Button.ButtonProperty.GUI_ITEM));
            }
        }

        return gui;
    }
}
