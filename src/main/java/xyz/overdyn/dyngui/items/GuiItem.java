package xyz.overdyn.dyngui.items;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.overdyn.dyngui.dupe.ItemMarker;
import xyz.overdyn.dyngui.placeholder.Placeholder;
import xyz.overdyn.dyngui.placeholder.context.PlaceholderContextImpl;

import java.util.*;
import java.util.function.Consumer;

/**
 * {@code GuiItem} represents a high-level GUI abstraction that binds
 * an {@link ItemStack} to inventory slots, runtime metadata,
 * placeholder-based rendering, and click handling logic.
 *
 * <p>This class is intentionally designed as a behavioral wrapper.
 * All visual and material-related configuration is delegated to
 * {@link ItemWrapper}, while {@code GuiItem} controls GUI state and interaction.</p>
 *
 * <p>Instances of this class are intended to be reusable, cloneable,
 * and safe to render dynamically for multiple players.</p>
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class GuiItem implements Cloneable {

    /**
     * The underlying {@link ItemWrapper} responsible for visual item configuration.
     * This object defines material, amount, name, lore, flags, model data, and updates.
     */
    private final ItemWrapper itemWrapper;

    /**
     * A collection of inventory slots this GUI item should occupy.
     * Multiple slots are supported for mirroring or repeated placement.
     */
    private final Collection<Integer> slots = new ArrayList<>();

    /**
     * Defines whether this item should be marked using {@link ItemMarker}
     * to prevent duplication, dragging, or unintended interaction.
     */
    private boolean marker;

    /**
     * Indicates whether this GUI item should be updated dynamically
     * during inventory refresh cycles.
     */
    private boolean update;

    /**
     * Optional logical identifier used by higher-level GUI systems
     * to reference or group GUI items.
     */
    private String key;

    /**
     * Arbitrary metadata map associated with this GUI item.
     * Intended for runtime state, flags, or contextual data.
     */
    private final Map<String, Object> metadata = new HashMap<>();

    /**
     * Metadata map specifically exposed to placeholder processing.
     * Values stored here may be accessed during rendering.
     */
    private final Map<String, Object> metadataPlaceholder = new HashMap<>();

    /**
     * Optional placeholder engine responsible for processing
     * display name and lore components per player.
     */
    private @Nullable Placeholder placeholderEngine;

    /**
     * Optional click handler executed when the player clicks this item.
     * The handler receives the raw {@link InventoryClickEvent}.
     */
    private @Nullable Consumer<InventoryClickEvent> clickHandler;

    /**
     * Constructs a new {@code GuiItem} based on an existing {@link ItemWrapper}.
     *
     * <p>The provided wrapper becomes the authoritative source of
     * all visual item configuration.</p>
     *
     * @param item the item wrapper to use
     */
    public GuiItem(@NotNull ItemWrapper item) {
        this.marker = true;
        this.itemWrapper = Objects.requireNonNull(item, "item");
    }

    /**
     * Constructs a {@code GuiItem} from a raw {@link ItemStack}.
     *
     * <p>The stack is wrapped internally into an {@link ItemWrapper}
     * and may be cloned or modified by the GUI system.</p>
     *
     * @param baseStack the base item stack
     */
    public GuiItem(@NotNull ItemStack baseStack) {
        this(new ItemWrapper(baseStack));
    }

    /**
     * Constructs a {@code GuiItem} from a material with default amount.
     *
     * <p>This is a convenience constructor for simple GUI elements.</p>
     *
     * @param material the item material
     */
    public GuiItem(@NotNull Material material) {
        this(new ItemWrapper(material));
    }

    /**
     * Constructs a {@code GuiItem} from a material and explicit amount.
     *
     * <p>This constructor is suitable for counters, stacks,
     * or quantity-based GUI elements.</p>
     *
     * @param material the item material
     * @param amount   the stack amount
     */
    public GuiItem(@NotNull Material material, int amount) {
        this(new ItemWrapper(material, amount));
    }

    /**
     * Replaces all currently assigned slots with the provided collection.
     *
     * <p>This operation clears existing slots before applying new ones.</p>
     *
     * @param slots the slot collection
     * @return this instance for chaining
     */
    public GuiItem setSlots(@NotNull Collection<Integer> slots) {
        this.slots.clear();
        this.slots.addAll(slots);
        return this;
    }

    /**
     * Adds a single inventory slot to this GUI item.
     *
     * <p>Duplicate slots are not automatically prevented.</p>
     *
     * @param slot the slot index
     * @return this instance for chaining
     */
    public GuiItem addSlot(int slot) {
        this.slots.add(slot);
        return this;
    }

    /**
     * Adds multiple inventory slots to this GUI item.
     *
     * <p>Slots are appended to the existing collection.</p>
     *
     * @param slots the slot collection
     * @return this instance for chaining
     */
    public GuiItem addSlots(@NotNull Collection<Integer> slots) {
        this.slots.addAll(slots);
        return this;
    }

    /**
     * Removes a specific slot from this GUI item.
     *
     * <p>If the slot is not present, this operation has no effect.</p>
     *
     * @param slot the slot index
     * @return this instance for chaining
     */
    public GuiItem remSlot(int slot) {
        this.slots.remove(slot);
        return this;
    }

    /**
     * Removes multiple slots from this GUI item.
     *
     * <p>Only slots present in the internal collection are removed.</p>
     *
     * @param slots the slots to remove
     * @return this instance for chaining
     */
    public GuiItem remSlots(@NotNull Collection<Integer> slots) {
        this.slots.removeAll(slots);
        return this;
    }

    /**
     * Clears all slot assignments for this GUI item.
     *
     * <p>After calling this method, the item will not be rendered
     * until new slots are assigned.</p>
     *
     * @return this instance for chaining
     */
    public GuiItem clearSlots() {
        this.slots.clear();
        return this;
    }

    /**
     * Enables or disables item marking.
     *
     * <p>Marked items are typically protected from duplication
     * or unintended inventory actions.</p>
     *
     * @param marker whether marking is enabled
     * @return this instance for chaining
     */
    public GuiItem setMarker(boolean marker) {
        this.marker = marker;
        return this;
    }

    /**
     * Returns whether this GUI item is marked.
     *
     * <p>Marked items are processed through {@link ItemMarker}.</p>
     *
     * @return {@code true} if marked
     */
    public boolean isMarker() {
        return marker;
    }

    /**
     * Returns the logical key associated with this GUI item.
     *
     * <p>The key may be {@code null} if not explicitly assigned.</p>
     *
     * @return the item key or {@code null}
     */
    public @Nullable String getKey() {
        return key;
    }

    /**
     * Assigns a logical key to this GUI item.
     *
     * <p>This key can be used by external systems
     * to identify or group GUI items.</p>
     *
     * @param key the key value
     * @return this instance for chaining
     */
    public GuiItem key(@Nullable String key) {
        this.key = key;
        return this;
    }

    /**
     * Sets whether this GUI item participates in update cycles.
     *
     * <p>Updated items may be re-rendered during GUI refresh.</p>
     *
     * @param update update flag
     * @return this instance for chaining
     */
    public GuiItem setUpdate(boolean update) {
        this.update = update;
        return this;
    }

    /**
     * Stores a metadata value under the specified name.
     *
     * <p>Metadata is not persisted and exists only at runtime.</p>
     *
     * @param name  metadata key
     * @param value metadata value
     * @return this instance for chaining
     */
    public GuiItem set(@NotNull String name, @Nullable Object value) {
        metadata.put(name, value);
        return this;
    }

    /**
     * Retrieves a metadata value by name.
     *
     * <p>If no value exists, {@code null} is returned.</p>
     *
     * @param name metadata key
     * @return stored value or {@code null}
     */
    public @Nullable Object get(@NotNull String name) {
        return metadata.get(name);
    }

    /**
     * Retrieves a typed metadata value.
     *
     * <p>The caller is responsible for providing the correct type.</p>
     *
     * @param name metadata key
     * @param type expected type
     * @return casted value or {@code null}
     */
    public @Nullable <T> T getAs(@NotNull String name, @NotNull Class<T> type) {
        Object o = metadata.get(name);
        return o == null ? null : type.cast(o);
    }

    /**
     * Retrieves a metadata value or returns a default if absent.
     *
     * <p>This method does not modify the underlying metadata map.</p>
     *
     * @param name metadata key
     * @param def  default value
     * @param type expected type
     * @return stored or default value
     */
    public <T> T getOrDefault(@NotNull String name, @Nullable T def, @NotNull Class<T> type) {
        Object o = metadata.get(name);
        return o == null ? def : type.cast(o);
    }

    /**
     * Checks whether a metadata entry exists.
     *
     * <p>This does not validate the stored value.</p>
     *
     * @param name metadata key
     * @return {@code true} if present
     */
    public boolean has(@NotNull String name) {
        return metadata.containsKey(name);
    }

    /**
     * Removes a metadata entry.
     *
     * <p>If the entry does not exist, no action is taken.</p>
     *
     * @param name metadata key
     * @return this instance for chaining
     */
    public GuiItem remove(@NotNull String name) {
        metadata.remove(name);
        return this;
    }

    /**
     * Returns the internal metadata map.
     *
     * <p>The returned map is mutable and reflects internal state.</p>
     *
     * @return metadata map
     */
    public Map<String, Object> metadata() {
        return metadata;
    }

    /**
     * Assigns a placeholder engine to this GUI item.
     *
     * <p>The engine is used during rendering to personalize
     * display name and lore per player.</p>
     *
     * @param placeholderEngine placeholder processor
     * @return this instance for chaining
     */
    public GuiItem placeholderEngine(@Nullable Placeholder placeholderEngine) {
        this.placeholderEngine = placeholderEngine;
        return this;
    }

    /**
     * Assigns a click handler to this GUI item.
     *
     * <p>The handler is invoked when the item is clicked
     * inside an inventory GUI.</p>
     *
     * @param clickHandler click consumer
     * @return this instance for chaining
     */
    public GuiItem onClick(@Nullable Consumer<InventoryClickEvent> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    /**
     * Invokes the click handler, if present.
     *
     * <p>This method does not cancel or modify the event.</p>
     *
     * @param event inventory click event
     */
    public void handleClick(@NotNull InventoryClickEvent event) {
        if (clickHandler != null) {
            clickHandler.accept(event);
        }
    }

    /**
     * Returns the base {@link ItemStack} representation of this GUI item.
     *
     * <p>If marking is enabled, the item is processed
     * through {@link ItemMarker}.</p>
     *
     * @return the base item stack
     */
    public ItemStack baseItemStack() {
        return marker ? ItemMarker.mark(itemWrapper.itemStack()) : itemWrapper.itemStack();
    }

    /**
     * Renders this GUI item for a specific player.
     *
     * <p>This includes placeholder processing of display name
     * and lore using the assigned placeholder engine.</p>
     *
     * @param player target player
     * @return rendered item stack
     */
    public ItemStack render(@Nullable OfflinePlayer player) {
        itemWrapper.update();

        PlaceholderContextImpl context =
                new PlaceholderContextImpl(player, metadataPlaceholder);

        ItemMeta meta = itemWrapper.itemStack().getItemMeta();
        if (meta == null) {
            return baseItemStack();
        }

        Component name = itemWrapper.displayName();
        List<Component> lore =
                itemWrapper.lore() != null ? new ArrayList<>(itemWrapper.lore()) : null;

        if (placeholderEngine != null) {
            if (name != null) {
                name = placeholderEngine.process(name, context);
            }
            if (lore != null) {
                lore = placeholderEngine.process(lore, context);
            }
        }

        if (name != null) {
            meta.displayName(name);
        }
        if (lore != null) {
            meta.lore(lore);
        }

        itemWrapper.itemStack().setItemMeta(meta);
        return baseItemStack();
    }

    /**
     * Returns the appropriate {@link ItemStack} for the given player.
     *
     * <p>If no player or placeholder engine is provided,
     * the base item is returned.</p>
     *
     * @param player target player
     * @return rendered or base item stack
     */
    public ItemStack itemStack(@Nullable OfflinePlayer player) {
        return (player == null || placeholderEngine == null)
                ? baseItemStack()
                : render(player);
    }

    /**
     * Sets the display name via the underlying {@link ItemWrapper}.
     *
     * <p>This is a convenience method for fluent configuration.</p>
     *
     * @param name display name component
     * @return this instance for chaining
     */
    public GuiItem name(@Nullable Component name) {
        this.itemWrapper.displayName(name);
        return this;
    }

    /**
     * Sets the lore via the underlying {@link ItemWrapper}.
     *
     * <p>This method replaces the entire lore list.</p>
     *
     * @param lore lore components
     * @return this instance for chaining
     */
    public GuiItem lore(@Nullable List<Component> lore) {
        this.itemWrapper.lore(lore);
        return this;
    }

    /**
     * Creates a deep clone of this GUI item.
     *
     * <p>The underlying {@link ItemWrapper} is cloned,
     * while metadata and configuration are copied.</p>
     *
     * @return cloned GUI item
     */
    @Override
    public GuiItem clone() {
        try {
            GuiItem clone = (GuiItem) super.clone();
            ItemWrapper clonedItem = this.itemWrapper.clone();
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

    /**
     * Applies metadata values from another map.
     *
     * <p>This method is intended for internal cloning logic.</p>
     *
     * @param meta metadata source
     * @return this instance
     */
    private GuiItem applyMetadata(Map<String, Object> meta) {
        this.metadata.putAll(meta);
        return this;
    }
}
