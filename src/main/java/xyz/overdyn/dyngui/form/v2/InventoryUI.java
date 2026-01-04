package xyz.overdyn.dyngui.form.v2;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import xyz.overdyn.dyngui.form.SkullCreator;
import xyz.overdyn.dyngui.form.v2.property.Property;
import xyz.overdyn.dyngui.form.v2.property.PropertyContainer;
import xyz.overdyn.dyngui.form.v2.property.SimpleProperty;
import xyz.overdyn.dyngui.items.GuiItem;
import xyz.overdyn.dyngui.items.ItemWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Полностью Property-driven InventoryUI с собственной системой требований и команд.
 */
public class InventoryUI implements PropertyContainer {

    private final Map<Property<?>, Object> metadata = new HashMap<>();

    @Override
    public Map<Property<?>, Object> metadata() {
        return metadata;
    }

    public <T> InventoryUI with(Property<T> property, T value) {
        meta(property, value);
        return this;
    }

    public void addMeta(String keyId, Object value) {
        Map<String, Object> meta = new HashMap<>(meta(MenuProperty.META));
        meta.put(keyId, value);
        meta(MenuProperty.META, meta);
    }

    public static ItemWrapper buildItem(Button button) {
        var materialRaw = button.meta(Button.ButtonProperty.MATERIAL);
        ItemStack itemStack;
        if (materialRaw.startsWith("basehead-")) {
            try {
                itemStack = SkullCreator.itemFromBase64(materialRaw.replace("basehead-", ""));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error creating custom skull: " + e.getMessage());
                itemStack = new ItemStack(SkullCreator.createSkull());
            }
        } else {
            itemStack = new ItemStack(Material.valueOf(materialRaw));
        }

        var itemWrapper = new ItemWrapper(itemStack);

        String rawName = button.meta(Button.ButtonProperty.DISPLAY_NAME);
        if (rawName != null && !rawName.isEmpty()) {
            itemWrapper.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(rawName));
        }

        List<String> rawLore = button.meta(Button.ButtonProperty.DISPLAY_LORE);
        itemWrapper.lore(
                rawLore == null ? null :
                        rawLore.stream()
                                .map(s -> (Component) LegacyComponentSerializer.legacyAmpersand().deserialize(s))
                                .collect(Collectors.toList())
        );

        itemWrapper.setAmount(button.meta(Button.ButtonProperty.AMOUNT));

        List<ItemFlag> flags = button.meta(Button.ButtonProperty.ITEM_FLAGS);
        if (flags != null) itemWrapper.flags(flags);

        Map<Enchantment, Integer> enchantmentsMap = button.meta(Button.ButtonProperty.ITEM_ENCHANTMENTS);
        if (enchantmentsMap != null && !enchantmentsMap.isEmpty()) {
            List<ItemWrapper.EnchantmentEntry> enchantmentEntries = new ArrayList<>();
            enchantmentsMap.forEach((ench, lvl) ->
                    enchantmentEntries.add(new ItemWrapper.EnchantmentEntry(ench, lvl))
            );
            itemWrapper.enchantments(enchantmentEntries);
        }

        Integer modelData = button.meta(Button.ButtonProperty.CUSTOM_MODEL_DATA);
        if (modelData != null && modelData != -1) itemWrapper.customModelData(modelData);

        return itemWrapper;
    }


    public InventoryUI build() {
        meta(MenuProperty.BUTTONS).forEach((key, value) -> {
            var itemWrapper = buildItem(value);
            var guiItem = new GuiItem(itemWrapper);
            guiItem.addSlot(value.meta(Button.ButtonProperty.SLOT));
            value.with(Button.ButtonProperty.GUI_ITEM, guiItem);
        });

        return this;
    }

    public interface Requirement {
        boolean test(Player player);
    }
        public record SimpleRequirement(String type, String input, String output, String permission,
                                        List<String> success_commands,
                                        List<String> failure_commands) implements Requirement {

            @Override
            public boolean test(Player player) {


                return true;
            }
        }

    public static class ClickRequirement implements Requirement {
        private final boolean anyClick;
        private final ClickType clickType;
        private final String type;
        private final String input;
        private final String output;
        private final String permission;
        private final List<String> successActions;
        private final List<String> failActions;

        public ClickRequirement(
                boolean anyClick,
                ClickType clickType,
                String type,
                String input,
                String output,
                String permission,
                List<String> successActions,
                List<String> failActions
        ) {
            this.anyClick = anyClick;
            this.clickType = clickType;
            this.type = type;
            this.input = input;
            this.output = output;
            this.permission = permission;
            this.successActions = successActions;
            this.failActions = failActions;
        }

        @Override
        public boolean test(Player player) {
            return true;
        }
    }

    /** Своя команда кнопки */
    public static class Command {
        private final boolean anyClick;
        private final ClickType clickType;
        private final List<String> actions;
        private final List<ClickRequirement> requirements;

        public Command(boolean anyClick, ClickType clickType, List<String> actions, List<ClickRequirement> requirements) {
            this.anyClick = anyClick;
            this.clickType = clickType;
            this.actions = actions;
            this.requirements = requirements;
        }

        public boolean canExecute(ClickType click) {
            if (anyClick) return true;
            return clickType == click;
        }
    }

    /** Своя кнопка с Property-driven API */
    public static class Button implements PropertyContainer {

        private final Map<Property<?>, Object> metadata = new HashMap<>();

        @Override
        public Map<Property<?>, Object> metadata() {
            return metadata;
        }

        public <T> Button with(Property<T> property, T value) {
            meta(property, value);
            return this;
        }

        public Button addCommand(Command command) {
            List<Command> commands = new ArrayList<>(meta(ButtonProperty.COMMANDS));
            commands.add(command);
            meta(ButtonProperty.COMMANDS, commands);
            return this;
        }

        public interface ButtonProperty {
            Property<String> ID = new SimpleProperty<>("button:id", UUID.randomUUID().toString());
            Property<Map<String, Object>> META = new SimpleProperty<>("button:meta", Map.of());
            Property<Integer> SLOT = new SimpleProperty<>("button:slot", 0);
            Property<String> DISPLAY_NAME = new SimpleProperty<>("button:display_name", "Default Button");
            Property<List<String>> DISPLAY_LORE = new SimpleProperty<>("button:display_lore", List.of());
            Property<String> MATERIAL = new SimpleProperty<>("button:display_lore", null);
            Property<Integer> PRIORITY = new SimpleProperty<>("button:priority", 0);
            Property<Integer> AMOUNT = new SimpleProperty<>("button:amount", 1);
            Property<Integer> CUSTOM_MODEL_DATA = new SimpleProperty<>("button:custom_model_data", -1);
            Property<List<ItemFlag>> ITEM_FLAGS = new SimpleProperty<>("button:item_flags", List.of());
            Property<Map<Enchantment, Integer>> ITEM_ENCHANTMENTS = new SimpleProperty<>("button:enchantments", Map.of());
            Property<ItemStack> ITEM_STACK = new SimpleProperty<>("button:item_stack", null);
            Property<GuiItem> GUI_ITEM = new SimpleProperty<>("button:gui_item", null);
            Property<List<SimpleRequirement>> VIEW_REQUIREMENTS = new SimpleProperty<>("button:view_requirements", List.of());
            Property<List<Command>> COMMANDS = new SimpleProperty<>("button:commands", List.of());
            Property<Boolean> UPDATE = new SimpleProperty<>("button:update", false);
            Property<Boolean> PLACEHOLDER = new SimpleProperty<>("button:placeholder", false);
            Property<Map<String, Object>> CUSTOM_VALUES = new SimpleProperty<>("button:custom_values", Map.of());
        }
    }

    public interface MenuProperty {
        Property<String> MENU_ID = new SimpleProperty<>("menu:id", UUID.randomUUID().toString());
        Property<String> TITLE = new SimpleProperty<>("menu:title", "Default Menu");
        Property<Integer> SIZE = new SimpleProperty<>("menu:size", -1);
        Property<InventoryType> TYPE = new SimpleProperty<>("menu:type", null);
        Property<Map<String, Object>> META = new SimpleProperty<>("menu:meta", Map.of());
        Property<Integer> UPDATE_INTERVAL = new SimpleProperty<>("menu:update_interval", -1);
        Property<Integer> REFRESH_INTERVAL = new SimpleProperty<>("menu:refresh_interval", -1);
        Property<List<String>> OPEN_COMMANDS = new SimpleProperty<>("menu:open_commands", List.of());
        Property<List<String>> CLOSE_COMMANDS = new SimpleProperty<>("menu:close_commands", List.of());
        Property<Integer> PRIORITY = new SimpleProperty<>("menu:priority", -1);
        Property<Map<String, Button>> BUTTONS = new SimpleProperty<>("menu:buttons", Map.of());
    }
}
