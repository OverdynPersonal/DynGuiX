package xyz.overdyn.dyngui.items.minecraft.meta;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import xyz.overdyn.dyngui.items.minecraft.ItemMinecraft;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a unified item metadata interface for fully configurable Bukkit {@link ItemStack}.
 * <p>
 * This interface provides methods for manipulating all common item properties including:
 * <ul>
 *     <li>Material and amount</li>
 *     <li>Display name and lore (both as raw {@link String} and Adventure {@link Component})</li>
 *     <li>Enchantments</li>
 *     <li>Item flags</li>
 *     <li>Custom model data</li>
 *     <li>Color (for leather armor)</li>
 *     <li>Skull textures via Base64</li>
 *     <li>Attribute modifiers</li>
 * </ul>
 * This interface is designed for Bukkit 1.18.2 API and supports safe, IDE-friendly, type-checked operations.
 * Implementations must correctly apply all metadata when building an {@link ItemStack}.
 * </p>
 */
public interface ItemData extends Cloneable {

    /* ---------------- Material & Amount ---------------- */

    /**
     * Sets the item material.
     *
     * @param material the Bukkit {@link Material} to set
     */
    void setMaterial(Material material);

    /**
     * Returns the currently set material.
     *
     * @return the Bukkit {@link Material} of this item
     */
    Material getMaterial();

    /**
     * Sets the stack amount.
     *
     * @param amount the number of items in the stack
     */
    void setAmount(int amount);

    /**
     * Returns the current stack amount.
     *
     * @return amount of items
     */
    int getAmount();

    /* ---------------- Display Name ---------------- */

    /**
     * Sets the display name using Adventure {@link Component}.
     *
     * @param component the display name component
     */
    void setDisplayName(Component component);

    /**
     * Sets the display name using raw string.
     *
     * @param name the display name as raw text
     */
    void setDisplayName(String name);

    /**
     * Returns the display name as Adventure {@link Component}.
     *
     * @return display name component
     */
    Component getDisplayName();

    /**
     * Returns the display name as raw string.
     *
     * @return display name text
     */
    String getRawDisplayName();

    /* ---------------- Lore ---------------- */

    /**
     * Sets the item lore using Adventure {@link Component} list.
     *
     * @param lore list of components
     */
    void setLore(List<Component> lore);

    /**
     * Sets the item lore using raw string list.
     *
     * @param lore list of lore strings
     */
    void setLoreStrings(List<String> lore);

    /**
     * Returns the lore as Adventure {@link Component} list.
     *
     * @return lore components
     */
    List<Component> getLore();

    /**
     * Returns the lore as raw string list.
     *
     * @return lore strings
     */
    List<String> getLoreStrings();

    /* ---------------- Enchantments ---------------- */

    /**
     * Adds an enchantment to the item.
     *
     * @param enchantment the Bukkit {@link Enchantment} to add
     * @param level       the enchantment level
     */
    void addEnchant(Enchantment enchantment, int level);

    /**
     * Removes an enchantment from the item.
     *
     * @param enchantment the Bukkit {@link Enchantment} to remove
     */
    void removeEnchant(Enchantment enchantment);


    default void removeEnchants() {
        getEnchants().keySet().forEach(this::removeEnchant);
    }

    /**
     * Returns the level of a specific enchantment.
     *
     * @param enchantment the Bukkit {@link Enchantment}
     * @return enchantment level, 0 if not present
     */
    int getEnchantLevel(Enchantment enchantment);

    /**
     * Returns all enchantments applied to the item.
     *
     * @return map of {@link Enchantment} to level
     */
    Map<Enchantment, Integer> getEnchants();

    /* ---------------- Item Flags ---------------- */

    /**
     * Adds an {@link ItemFlag} to hide certain item properties in-game.
     *
     * @param flag the {@link ItemFlag} to add
     */
    void addItemFlag(ItemFlag flag);


    default void addItemFlags(List<ItemFlag> flags) {
        flags.forEach(this::addItemFlag);
    }

    /**
     * Removes an {@link ItemFlag}.
     *
     * @param flag the {@link ItemFlag} to remove
     */
    void removeItemFlag(ItemFlag flag);

    /**
     * Checks if an {@link ItemFlag} is applied.
     *
     * @param flag the {@link ItemFlag} to check
     * @return true if present
     */
    boolean hasItemFlag(ItemFlag flag);

    /**
     * Returns all applied {@link ItemFlag}s.
     *
     * @return set of item flags
     */
    Set<ItemFlag> getItemFlags();

    /* ---------------- Custom Model Data ---------------- */

    /**
     * Sets the custom model data of the item.
     *
     * @param data integer representing the custom model
     */
    void setCustomModelData(Integer data);

    /**
     * Returns the custom model data.
     *
     * @return custom model data integer, null if not set
     */
    Integer getCustomModelData();

    /* ---------------- Color ---------------- */

    /**
     * Sets the color of leather armor.
     *
     * @param color the Bukkit {@link Color} to apply
     */
    void setColor(Color color);

    /**
     * Returns the color applied to leather armor.
     *
     * @return the Bukkit {@link Color}, null if not leather armor or not set
     */
    Color getColor();

    /* ---------------- Skull / Player Head ---------------- */

    /**
     * Sets the skull owner (player head) by player name.
     *
     * @param playerName name of the player
     */
    void setSkullOwner(String playerName);

    /**
     * Sets the skull texture using a Base64 string.
     *
     * @param base64 Base64 representation of the skin texture
     */
    void setSkullTextureBase64(String base64);

    /**
     * Checks if a custom skull texture is set.
     *
     * @return true if Base64 texture is applied
     */
    boolean hasCustomSkullTexture();

    /* ---------------- Attribute Modifiers ---------------- */

    /**
     * Adds an attribute modifier to the item.
     *
     * @param attribute {@link Attribute} to modify
     * @param modifier  {@link AttributeModifier} object
     */
    void addAttribute(Attribute attribute, AttributeModifier modifier);

    /**
     * Removes all attribute modifiers of a specific {@link Attribute} from the item.
     *
     * <p>Unlike Bukkit's native API, you don't need to provide a UUID.
     * All modifiers for the given attribute will be removed automatically.</p>
     *
     * @param attribute the {@link Attribute} whose modifiers should be removed
     */
    void removeAttribute(Attribute attribute);


    /**
     * Returns all attribute modifiers applied.
     *
     * @return map of {@link Attribute} to set of {@link AttributeModifier}s
     */
    Map<Attribute, Set<AttributeModifier>> getAttributes();

    /* ---------------- Build / Apply ---------------- */

    /**
     * Asynchronously builds the ItemStack.
     * High-performance choice for complex GUIs to keep the Main Thread free.
     *
     * @return a CompletableFuture containing the finished ItemStack
     */
    CompletableFuture<ItemStack> buildItemStackAsync();

    String getSkullTextureBase64();

    ItemMinecraft clone();

    /**
     * Builds a Bukkit {@link ItemStack} with all properties applied.
     *
     * @return fully configured ItemStack
     */
    ItemStack buildItemStack();
}
