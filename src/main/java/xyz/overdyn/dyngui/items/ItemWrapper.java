package xyz.overdyn.dyngui.items;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.overdyn.dyngui.placeholder.Placeholder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * High-level wrapper around a {@link ItemStack} that centralizes all display and
 * interaction logic for GUI items. It supports dynamic names, lore, placeholder
 * processing, custom model data, enchant glint, flags and click handlers.
 * <p>
 * This class is meant to be used in inventory-based menus where items must be
 * updated frequently and consistently. It offers both automatic and manual
 * update modes to balance performance and convenience.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ItemWrapper implements Cloneable {

    /**
     * Underlying {@link ItemStack} instance that is being wrapped and manipulated.
     * All changes applied by this wrapper are eventually written into this stack
     * when {@link #update()} is executed or when auto-flush mode is enabled.
     * <p>
     * This object is what actually appears in the player's inventory and is used
     * by Bukkit when rendering or interacting with the item in GUIs.
     */
    private ItemStack itemStack;

    /**
     * Current {@link Material} assigned to the wrapped {@link ItemStack}. This
     * determines the core type of the item, such as STONE, PAPER or DIAMOND_SWORD.
     * <p>
     * Changing this value allows you to morph the item into another type, which
     * is particularly useful in GUI systems to visualize toggles or states.
     */
    private Material material;

    /**
     * Display name of the item represented as a {@link Component}. This value
     * can be pre-processed or dynamically modified using a {@link xyz.overdyn.dyngui.placeholder.Placeholder}
     * when the update logic is executed.
     * <p>
     * The stored value is considered the base form, while any placeholder
     * resolution happens inside the update cycle before it is written to meta.
     */
    private Component displayName;

    /**
     * Lore lines associated with this item represented as a list of Adventure
     * {@link Component}s. These lines are rendered under the display name inside
     * the tooltip when a player hovers over the item in an inventory.
     * <p>
     * As with the display name, lore can also be processed by a
     * {@link Placeholder} to show context-sensitive information such as
     * balances, cooldown timers or custom GUI hints.
     */
    private List<Component> displayLore;

    /**
     * Custom model data value that can be used by resource packs to select an
     * alternative visual model. When this value is non-null, client-side packs
     * may override the default icon with a fully custom one.
     * <p>
     * If the value is null, the underlying item meta will not store a model
     * override, and the default visual representation will be used instead.
     */
    private Integer customModelData;

    /**
     * Flag indicating whether this item should visually appear enchanted or not.
     * The effect is cosmetic only and does not necessarily imply any real
     * enchantments applied to the item in terms of gameplay mechanics.
     * <p>
     * This is widely used in GUI design as a way to highlight selected entries,
     * important buttons, or active states that require the player's attention.
     */
    private boolean enchanted;

    /**
     * List of {@link ItemFlag} values that control which aspects of the item
     * metadata are visible in the tooltip. For example, certain flags may hide
     * enchant descriptions, attribute modifiers or unnecessary data.
     * <p>
     * When this list is null, no flags are explicitly managed by the wrapper,
     * and whatever flags the item had originally will remain unchanged.
     */
    private List<ItemFlag> flags;

    /**
     * Creates a new wrapper around the specified {@link ItemStack} and serializer.
     * When the serializer is null, a sensible default is chosen so that legacy
     * and modern text formats can still be processed correctly.
     * <p>
     * This constructor is best used when you already have a prepared stack and
     * need to attach GUI-specific behavior like placeholder processing or clicks.
     *
     * @param itemStack  the stack to wrap, must not be {@code null}
     */
    public ItemWrapper(@NotNull final ItemStack itemStack) {
        Preconditions.checkArgument(itemStack != null, "ItemStack cannot be null");
        this.itemStack = itemStack;
        this.material = itemStack.getType();
    }

    /**
     * Creates a new wrapper by first instantiating an {@link ItemStack} based
     * on the given material and amount. A custom serializer can be supplied,
     * or left as null to indicate that default behavior should be used.
     * <p>
     * This constructor is practical when you are defining GUI items directly
     * based on a material, and you do not yet have an ItemStack instance.
     *
     * @param material   the material used to construct the underlying stack
     * @param amount     the quantity to set on the created ItemStack
     */
    public ItemWrapper(@NotNull final Material material, final int amount) {
        this(new ItemStack(material, amount));
    }

    /**
     * Creates a wrapper that internally constructs a single-item {@link ItemStack}
     * of the given material, using the default serializer configuration.
     * This is the simplest constructor for quickly defining GUI icons.
     * <p>
     * Use this for typical static menu buttons and display elements where
     * specific serializer settings are not important.
     *
     * @param material the material used to create a new ItemStack
     */
    public ItemWrapper(@NotNull final Material material) {
        this(material, 1);
    }

    /**
     * Creates a new {@link Builder} instance bound to the given material and
     * serializer. This enables explicit serializer configuration for items
     * constructed using the builder API.
     * <p>
     * The builder will use the supplied serializer for converting all string
     * values into their corresponding {@link Component} forms.
     *
     * @param material   the base material for the item
     * @return a new builder bound to the given material and serializer
     */
    public static Builder builder(@NotNull Material material) {
        Preconditions.checkArgument(material != null, "Material cannot be null");
        return new Builder(material);
    }

    /**
     * Sets the amount on the underlying {@link ItemStack} without explicitly
     * updating any of the display metadata. This operation affects only the
     * actual stack size stored in the inventory.
     * <p>
     * You can combine this with manual {@link #update()} calls if you wish to
     * synchronize display-related information afterwards.
     *
     * @param amount new item stack amount to assign
     */
    public void setAmount(int amount) {
        itemStack.setAmount(amount);
    }

    /**
     * Updates all relevant metadata fields on the underlying {@link ItemStack}
     * including name, lore, custom model data, flags and enchant visual state.
     * Placeholders are processed if a {@link Placeholder} is assigned.
     * <p>
     * This is the core method that should be invoked once all desired changes
     * have been made, especially when auto-flush mode is disabled.
     *
     */
    public void update() {
        var cachedMeta = itemStack.getItemMeta();

        cachedMeta.displayName(displayName);
        cachedMeta.lore(displayLore);
        cachedMeta.setCustomModelData(customModelData);

        if (enchanted) {
            cachedMeta.addEnchant(Enchantment.LURE, 1, true);
        } else {
            cachedMeta.removeEnchant(Enchantment.LURE);
        }

        if (flags != null) {
            if (!cachedMeta.getItemFlags().isEmpty()) {
                for (ItemFlag f : cachedMeta.getItemFlags()) {
                    cachedMeta.removeItemFlags(f);
                }
            }
            cachedMeta.addItemFlags(flags.toArray(new ItemFlag[0]));
        }

        itemStack.setType(material);
        itemStack.setItemMeta(cachedMeta);
    }


    /**
     * Applies a modification to this wrapper by executing the given consumer and
     * then returns the same instance. This enables readable, fluent-style chains
     * when configuring items programmatically.
     * <p>
     * The consumer is allowed to call any method on the wrapper, including other
     * mutators, before control returns to the caller.
     *
     * @param consumer action that receives this wrapper instance, may be null
     * @return this wrapper instance for further chaining
     */
    public ItemWrapper apply(Consumer<ItemWrapper> consumer) {
        if (consumer != null) consumer.accept(this);
        return this;
    }

    /**
     * Returns the underlying {@link ItemStack} managed by this wrapper. This
     * object is what should be placed into inventories when constructing GUIs.
     * <p>
     * Callers may read or inspect the stack directly, but should prefer using
     * the wrapper's mutation methods to keep metadata consistent.
     *
     * @return the wrapped ItemStack instance
     */
    @NotNull
    public ItemStack itemStack() {
        return itemStack;
    }

    /**
     * Replaces the underlying {@link ItemStack} instance with a new one and
     * resets internal metadata cache. This does not copy any fields from the
     * previous stack into the new one.
     * <p>
     * After calling this, subsequent updates will be applied to the newly
     * assigned stack based on the wrapper's current configuration.
     *
     * @param itemStack new ItemStack instance to wrap, must not be {@code null}
     */
    public void itemStack(@NotNull ItemStack itemStack) {
        Preconditions.checkArgument(itemStack != null, "ItemStack cannot be null");
        this.itemStack = itemStack;
        this.material = itemStack.getType();
        update();
    }

    /**
     * Returns the {@link Material} currently associated with the wrapped
     * {@link ItemStack}. This indicates the core item type as rendered in GUIs.
     * <p>
     * Modifying this material through {@link #material(Material)} will change
     * how the item appears visually to players in inventories.
     *
     * @return current material assigned to this wrapper
     */
    @NotNull
    public Material material() {
        return material;
    }

    /**
     * Sets a new {@link Material} value for the wrapped item and schedules an
     * update according to the auto-flush setting. This will change how the
     * icon appears to the player.
     * <p>
     * The method ensures the provided material is not {@code null} and updates
     * the internal material reference either immediately or lazily.
     *
     * @param material new material to assign to the item
     */
    public void material(@NotNull Material material) {
        Preconditions.checkArgument(material != null, "Material cannot be null");
        this.material = material;
        update();
    }

    /**
     * Returns the currently stored display name component for this item. This
     * may represent either the base form or the last placeholder-processed
     * value, depending on the most recent update operation.
     * <p>
     * If the display name has never been set, this method may return
     * {@code null}, indicating that no custom name is applied.
     *
     * @return current display name component, or {@code null} if none
     */
    public Component displayName() {
        return displayName;
    }

    /**
     * Returns the base display name value stored on this wrapper without any
     * guarantee about placeholder processing or player context.
     * <p>
     * This method is semantically identical to {@link #displayName()} but can
     * be used for clarity in code that distinguishes base and resolved states.
     *
     * @return the base name component, or {@code null} if not set
     */
    public Component baseDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name using a {@link Component} instance and schedules an
     * update according to auto-flush rules. The provided component is stored as
     * the new base value.
     * <p>
     * When placeholders are in use, the actual rendered name may later differ
     * from this base component after processing.
     *
     * @param displayName new display name component, or {@code null} to clear
     */
    public void displayName(@Nullable Component displayName) {
        this.displayName = displayName;
        update();
    }

    /**
     * Returns the current lore list being used by this item. The list may have
     * been processed through placeholder resolution, depending on previous
     * update operations and the presence of a {@link Placeholder}.
     * <p>
     * The returned list may be {@code null} if no lore has been configured
     * on this wrapper.
     *
     * @return list of lore components, or {@code null} if none
     */
    public List<Component> lore() {
        return displayLore;
    }

    /**
     * Returns the base lore list stored within this wrapper. Similar to
     * {@link #lore()}, this method is mainly provided for semantic clarity
     * when differentiating between base and resolved states.
     * <p>
     * If no lore has ever been assigned, this method may return {@code null}
     * to indicate that the item has no lore lines configured.
     *
     * @return base lore components, or {@code null} if unset
     */
    public List<Component> baseLore() {
        return displayLore;
    }

    /**
     * Sets the lore list for this item using a list of {@link Component}
     * instances. This method overwrites any previously assigned lore and
     * schedules an update based on auto-flush behavior.
     * <p>
     * Passing {@code null} removes all lore from the item, causing the tooltip
     * to no longer display any additional lines under the name.
     *
     * @param lore list of components representing lore lines, or {@code null}
     */
    public void lore(@Nullable List<Component> lore) {
        this.displayLore = lore;
        update();
    }

    /**
     * Returns the custom model data currently associated with this item. When
     * non-null, this numeric value informs client-side resource packs which
     * model variant should be used for rendering.
     * <p>
     * If the value is {@code null}, the wrapper does not enforce any custom
     * model override on the underlying {@link ItemMeta}.
     *
     * @return current custom model data, or {@code null} if none
     */
    public Integer customModelData() {
        return customModelData;
    }

    /**
     * Sets the custom model data value to be written into the item meta and
     * schedules an update according to the auto-flush configuration. This
     * value is used in conjunction with resource packs for custom models.
     * <p>
     * Passing {@code null} removes any existing custom model override from the
     * item and restores the default model behavior.
     *
     * @param data new custom model data value, or {@code null} to clear
     */
    public void customModelData(@Nullable Integer data) {
        this.customModelData = data;
        update();
    }

    /**
     * Returns whether this item should visually appear enchanted in-game. This
     * does not necessarily reflect real enchantments applied to the item, and is
     * typically only used to show the enchantment glint.
     * <p>
     * The returned value is purely a visual configuration hint stored within
     * the wrapper.
     *
     * @return {@code true} if enchant glint should be shown, {@code false} otherwise
     */
    public boolean enchanted() {
        return enchanted;
    }

    /**
     * Configures whether this item should appear visually enchanted and
     * schedules a metadata update according to the auto-flush flag. The visual
     * effect is applied using a dummy enchantment internally.
     * <p>
     * Setting this to {@code false} removes the forced enchant appearance and
     * attempts to restore the previous non-glint state.
     *
     * @param enchanted {@code true} to show enchant glint, {@code false} to hide
     */
    public void enchanted(boolean enchanted) {
        this.enchanted = enchanted;
        update();
    }

    /**
     * Returns the list of {@link ItemFlag} values managed by this wrapper. These
     * flags control which parts of item meta are visible to the player, such as
     * attributes and enchant descriptions.
     * <p>
     * This list may be {@code null} if the wrapper has not been configured to
     * manage any flags itself.
     *
     * @return list of item flags, or {@code null} if none are defined
     */
    public List<ItemFlag> flags() {
        return flags;
    }

    /**
     * Assigns a list of {@link ItemFlag} values that will be applied to the
     * underlying {@link ItemMeta} during update operations. Existing flags managed
     * by the wrapper are overwritten by this call.
     * <p>
     * Passing {@code null} indicates that the wrapper should not explicitly
     * manipulate flags and will simply leave the current item meta configuration.
     *
     * @param flags list of item flags to apply, or {@code null} to disable control
     */
    public void flags(@Nullable List<ItemFlag> flags) {
        this.flags = flags;
        update();
    }

    /**
     * Assigns {@link ItemFlag} values using a varargs array, converting it into
     * an internal list representation. This is a convenience overload for
     * concise code when defining flags inline.
     * <p>
     * Passing {@code null} will be treated the same way as calling
     * {@link #flags(List)} with {@code null}.
     *
     * @param flags array of item flags to apply, or {@code null}
     */
    public void flags(ItemFlag... flags) {
        this.flags = flags == null ? null : Arrays.asList(flags);
        update();
    }

    /**
     * Creates a shallow clone of this wrapper, duplicating the underlying
     * {@link ItemStack} and copying display-related fields. Cached metadata is
     * cleared so that the cloned wrapper will re-initialize it lazily.
     * <p>
     * This method is useful when you need similar items with independent
     * state tracking, such as repeated GUI entries.
     *
     * @return a new {@link ItemWrapper} instance mirroring this one
     */
    @Override
    public ItemWrapper clone() {
        try {
            ItemWrapper clone = (ItemWrapper) super.clone();
            clone.itemStack = this.itemStack.clone();
            clone.displayName = this.displayName;
            clone.material = this.material;
            clone.customModelData = this.customModelData;
            clone.enchanted = this.enchanted;
            clone.displayLore = this.displayLore == null ? null : new ArrayList<>(this.displayLore);
            clone.flags = this.flags == null ? null : new ArrayList<>(this.flags);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builder class for constructing {@link ItemWrapper} instances in a fluent,
     * chainable manner. It encapsulates all configurable properties before
     * producing a fully initialized wrapper.
     * <p>
     * Use this when you need to define complex GUI items with many attributes
     * in a clean and readable style.
     */
    public static class Builder {

        /**
         * Base material from which the wrapped {@link ItemStack} will be created.
         * This field is mandatory and identifies the fundamental item type to use.
         * <p>
         * All items produced by this builder will share the same material value
         * unless the builder instance is discarded and recreated.
         */
        private final Material material;

        /**
         * Amount of items in the resulting {@link ItemStack}. For typical GUI
         * usage this is often set to one, but it can be adjusted to represent
         * counts or highlight unusual states.
         * <p>
         * The amount will be passed to the underlying ItemWrapper constructor
         * when {@link #build()} is invoked.
         */
        private int amount = 1;

        /**
         * Display name component to apply to the built item. This name may be
         * created from a string or assigned as a {@link Component} directly,
         * depending on which builder methods are used.
         * <p>
         * When left null, the resulting wrapper will not override the default
         * name of the constructed {@link ItemStack}.
         */
        private Component displayName;

        /**
         * List of lore components to attach to the built item. These components
         * represent each line of tooltip text shown beneath the item name.
         * <p>
         * If this field remains null, the resulting item will not have any lore
         * explicitly configured by the builder.
         */
        private List<Component> displayLore;

        /**
         * Custom model data value specifying which model variant should be used
         * by resource packs for the built item. It is optional and may be null.
         * <p>
         * When set, the resulting {@link ItemWrapper} will apply this model
         * data to the underlying item meta during its first update.
         */
        private Integer customModelData;

        /**
         * Flag controlling whether the built item should visually appear to be
         * enchanted. The effect is purely cosmetic and is used frequently in
         * GUI design to draw player attention.
         * <p>
         * If true, the wrapper will apply a dummy enchantment upon update so
         * that the enchant glint effect is rendered by the client.
         */
        private boolean enchanted;

        /**
         * List of {@link ItemFlag} values to apply to the built item. Flags
         * control which parts of item metadata are visible in the tooltip,
         * allowing clean and minimal GUI representations.
         * <p>
         * A null value means the builder will not explicitly define flags and
         * will leave them controlled by the underlying Bukkit mechanics.
         */
        private List<ItemFlag> flags;

        /**
         * Constructs a new builder for an {@link ItemWrapper} using the given
         * material and the default serializer configuration. This supports
         * the {@link ItemWrapper#builder(Material)} factory method.
         * <p>
         * The serializer can still be changed later by replacing the field,
         * although typical usage relies on the initial default.
         *
         * @param material material for all items produced by this builder
         */
        private Builder(@NotNull Material material) {
            this.material = material;
        }

        /**
         * Sets the stack amount for the item being built. This determines how
         * many items will appear in a single stack in the player's inventory.
         * <p>
         * For GUI elements this is often left at one, but it can also be used
         * to visually represent counts or intensities.
         *
         * @param amount new amount for the resulting item stack
         * @return this builder instance for chaining
         */
        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Assigns a display name component to the item being built, replacing
         * any previously defined name. The component is stored directly and
         * later applied via the {@link ItemWrapper} update logic.
         * <p>
         * Use this overload when you already have a {@link Component} instance
         * prepared and do not need string deserialization.
         *
         * @param displayName new display name component, or {@code null} to clear
         * @return this builder instance for chaining
         */
        public Builder displayName(@Nullable Component displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Defines the lore for the item being built using a list of {@link Component}
         * instances. The list may be null to indicate that no lore should be set.
         * <p>
         * These components will later be applied to the produced {@link ItemWrapper}
         * during its initial update.
         *
         * @param lore list of lore components, or {@code null} to remove lore
         * @return this builder instance for chaining
         */
        public Builder lore(@Nullable List<Component> lore) {
            this.displayLore = lore;
            return this;
        }

        /**
         * Sets the custom model data value that should be applied to the built
         * item. This value will later be written into the {@link ItemMeta} by
         * the constructed {@link ItemWrapper}.
         * <p>
         * Passing {@code null} clears any custom model data configuration from
         * the builder so that the resulting item uses its default model.
         *
         * @param customModelData numeric custom model identifier, or {@code null}
         * @return this builder instance for chaining
         */
        public Builder customModelData(@Nullable Integer customModelData) {
            this.customModelData = customModelData;
            return this;
        }

        /**
         * Defines whether the built item should appear visually enchanted. The
         * resulting {@link ItemWrapper} will store this configuration and apply
         * the appropriate visual effect during metadata updates.
         * <p>
         * Setting this to {@code false} will instruct the wrapper not to force
         * the enchantment glint on the final item.
         *
         * @param enchanted {@code true} to show glint, {@code false} otherwise
         * @return this builder instance for chaining
         */
        public Builder enchanted(boolean enchanted) {
            this.enchanted = enchanted;
            return this;
        }

        /**
         * Defines the {@link ItemFlag} set to apply to the built item using a
         * varargs array. The array is converted into a list and stored for later
         * application by the {@link ItemWrapper}.
         * <p>
         * Passing {@code null} indicates that the builder should not enforce any
         * specific set of flags on the resulting item.
         *
         * @param flags array of flags to assign, or {@code null}
         * @return this builder instance for chaining
         */
        public Builder flags(ItemFlag... flags) {
            this.flags = flags == null ? null : Arrays.asList(flags);
            return this;
        }


        /**
         * Finalizes the builder configuration and constructs a new {@link ItemWrapper}
         * instance. All configured fields are applied to the wrapper before it is
         * returned to the caller.
         * <p> <p>
         * After construction, the wrapper's {@link ItemWrapper#update()} method
         * is invoked once to ensure that the underlying {@link ItemStack} is fully
         * synchronized with the configured properties.
         *
         * @return a fully initialized {@link ItemWrapper} based on this builder
         */
        public ItemWrapper build() {
            ItemWrapper wrapper = new ItemWrapper(material, amount);
            wrapper.displayName = displayName;
            wrapper.displayLore = displayLore;
            wrapper.customModelData = customModelData;
            wrapper.enchanted = enchanted;
            wrapper.flags = flags;
            wrapper.update();
            return wrapper;
        }
    }
}
