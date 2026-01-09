package xyz.overdyn.dyngui.items;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.overdyn.dyngui.dupe.ItemMarker;
import xyz.overdyn.dyngui.items.minecraft.ItemMinecraft;
import xyz.overdyn.dyngui.items.minecraft.meta.ItemData;
import xyz.overdyn.dyngui.placeholder.Placeholder;
import xyz.overdyn.dyngui.placeholder.context.PlaceholderContextImpl;

import java.util.*;
import java.util.function.Consumer;

/**
 * {@code GuiItem} represents a high-level GUI element abstraction
 * that binds a visual {@link ItemStack} to inventory slots,
 * runtime metadata, placeholder rendering, and click handling.
 *
 * <p>This class is intentionally separated from raw item logic.
 * All visual configuration (material, name, lore, flags, model data)
 * is delegated to {@link ItemWrapper}, while {@code GuiItem}
 * focuses exclusively on GUI behavior and state.</p>
 *
 * <p>{@code GuiItem} instances are designed to be long-lived,
 * reusable, and safe to update across multiple GUI refresh cycles.</p>
 */
@Getter
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class GuiItem implements Cloneable {

    private final Collection<Integer> slots = new ArrayList<>();
    private final @NotNull ItemData data;
    private boolean marker;
    private boolean update;
    private String key;
    private final Map<String, Object> metadata = new HashMap<>();
    private final Map<String, Object> metadataPlaceholder = new HashMap<>();

    private @Nullable Placeholder placeholderEngine;
    private @Nullable Consumer<InventoryClickEvent> clickHandler;

    @Deprecated(since = "1.0.2.3", forRemoval = true)
    public static GuiItem fromLegacy(@NotNull ItemWrapper item) {
        return LegacyItemAdapter.convert(item);
    }

    public GuiItem(@NotNull ItemData data) {
        this.marker = true;
        this.data = data;
    }

    public GuiItem(@NotNull ItemStack baseStack) {
        this(new ItemMinecraft(baseStack));
    }

    public GuiItem(@NotNull Material material) {
        this(new ItemMinecraft(material));
    }

    public GuiItem(@NotNull Material material, int amount) {
        this(new ItemMinecraft(material, amount));
    }

    public GuiItem setSlots(@NotNull Collection<Integer> slots) {
        this.slots.clear();
        this.slots.addAll(slots);
        return this;
    }

    public GuiItem addSlot(int slot) {
        this.slots.add(slot);
        return this;
    }

    public GuiItem addSlots(@NotNull Collection<Integer> slots) {
        this.slots.addAll(slots);
        return this;
    }

    public GuiItem remSlot(int slot) {
        this.slots.remove(slot);
        return this;
    }

    public GuiItem remSlots(@NotNull Collection<Integer> slots) {
        this.slots.removeAll(slots);
        return this;
    }

    public GuiItem clearSlots() {
        this.slots.clear();
        return this;
    }

    public GuiItem setMarker(boolean marker) {
        this.marker = marker;
        return this;
    }

    public boolean isMarker() {
        return marker;
    }

    public String key() {
        return key;
    }

    public GuiItem key(@Nullable String key) {
        this.key = key;
        return this;
    }

    public GuiItem setUpdate(boolean update) {
        this.update = update;
        return this;
    }

    public GuiItem set(@NotNull String name, @Nullable Object value) {
        metadata.put(name, value);
        return this;
    }

    @Nullable
    public Object get(@NotNull String name) {
        return metadata.get(name);
    }

    @Nullable
    public <T> T getAs(@NotNull String name, @NotNull Class<T> type) {
        Object o = metadata.get(name);
        if (o == null) return null;
        return type.cast(o);
    }

    public <T> T getOrDefault(@NotNull String name, @Nullable T def, @NotNull Class<T> type) {
        Object o = metadata.get(name);
        return o == null ? def : type.cast(o);
    }

    public boolean has(@NotNull String name) {
        return metadata.containsKey(name);
    }

    public GuiItem remove(@NotNull String name) {
        metadata.remove(name);
        return this;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public GuiItem placeholderEngine(Placeholder placeholderEngine) {
        this.placeholderEngine = placeholderEngine;
        return this;
    }

    public GuiItem onClick(@Nullable Consumer<InventoryClickEvent> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public void handleClick(@NotNull InventoryClickEvent event) {
        if (clickHandler != null) clickHandler.accept(event);
    }

    public ItemStack baseItemStack() {
        if (isMarker()) return ItemMarker.mark(data.buildItemStack());
        return data.buildItemStack();
    }

    public ItemData getItemData() {
        return data;
    }

    public ItemStack render(@Nullable OfflinePlayer player) {
        try {
            var cachedMeta = data.buildItemStack().getItemMeta();
            if (cachedMeta == null) return baseItemStack();

            if (placeholderEngine != null) {
                try {
                    var context = new PlaceholderContextImpl(player, metadataPlaceholder);

                    Component name = data.getDisplayName();
                    List<Component> lore = data.getLore() != null ? new ArrayList<>(data.getLore()) : null;

                    if (name != null) {
                        name = placeholderEngine.process(name, context);
                        cachedMeta.displayName(name);
                    }
                    if (lore != null) {
                        lore = placeholderEngine.process(lore, context);
                        cachedMeta.lore(lore);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            data.buildItemStack().setItemMeta(cachedMeta);

            return baseItemStack();
        } catch (Exception e) {
            e.printStackTrace();
            return baseItemStack();
        }
    }

    public ItemStack itemStack(@Nullable OfflinePlayer player) {
        if (player == null || placeholderEngine == null) {
            return baseItemStack();
        }
        return render(player);
    }

    public GuiItem name(@Nullable Component name) {
        this.data.setDisplayName(name);
        return this;
    }

    public GuiItem lore(@Nullable List<Component> lore) {
        this.data.setLore(lore);
        return this;
    }

    public GuiItem setName(@Nullable String name) {
        this.data.setDisplayName(name);
        return this;
    }

    public GuiItem setLore(@Nullable List<String> lore) {
        this.data.setLoreStrings(lore);
        return this;
    }

    @Override
    public GuiItem clone() {
        try {
            GuiItem clone = (GuiItem) super.clone();
            ItemData clonedItem = this.data.clone();
            return new GuiItem(clonedItem)
                    .placeholderEngine(this.placeholderEngine)
                    .onClick(this.clickHandler)
                    .key(this.key)
                    .setSlots(this.slots)
                    .applyMetadata(this.metadata);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private GuiItem applyMetadata(Map<String, Object> meta) {
        this.metadata.putAll(meta);
        return this;
    }
}
