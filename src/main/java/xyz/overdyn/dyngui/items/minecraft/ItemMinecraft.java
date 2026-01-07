package xyz.overdyn.dyngui.items.minecraft;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import xyz.overdyn.dyngui.items.minecraft.meta.ItemData;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemMinecraft implements ItemData, Cloneable {

    private static final boolean IS_PAPER = checkPaperPresence();
    private static final Map<String, Object> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static Field skullProfileField;

    private ItemStack internalStack;
    private Material material = Material.STONE;
    private int amount = 1;

    private Component displayName;
    private String rawDisplayName;
    private List<Component> lore;
    private List<String> loreStrings;

    private String skullOwner;
    private String skullTextureBase64;
    private Object cachedProfile;

    public String getSkullTextureBase64() {
        return skullTextureBase64;
    }

    public ItemMinecraft() {
        this.internalStack = new ItemStack(material, amount);
    }

    public ItemMinecraft(Material material, int amount) {
        this.internalStack = new ItemStack(material, amount);
        this.material = material;
        this.amount = amount;
    }

    private static boolean checkPaperPresence() {
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void updateMeta(Consumer<org.bukkit.inventory.meta.ItemMeta> consumer) {
        org.bukkit.inventory.meta.ItemMeta meta = internalStack.getItemMeta();
        if (meta != null) {
            consumer.accept(meta);
            internalStack.setItemMeta(meta);
        }
    }

    @Override
    public void setMaterial(Material material) {
        this.material = material != null ? material : Material.STONE;
        this.internalStack.setType(this.material);
    }

    @Override
    public void setAmount(int amount) {
        this.amount = Math.max(1, amount);
        this.internalStack.setAmount(this.amount);
    }

    @Override
    public void setDisplayName(Component component) {
        this.displayName = component;
        this.rawDisplayName = null;
        updateMeta(meta -> meta.displayName(component));
    }

    @Override
    public void setDisplayName(String name) {
        this.rawDisplayName = name;
        this.displayName = null;
        Component comp = name != null ? SERIALIZER.deserialize(name) : null;
        updateMeta(meta -> meta.displayName(comp));
    }

    @Override
    public void setLore(List<Component> lore) {
        this.lore = lore;
        this.loreStrings = null;
        updateMeta(meta -> meta.lore(lore));
    }

    @Override
    public void setLoreStrings(List<String> lore) {
        this.loreStrings = lore;
        this.lore = null;
        List<Component> comps = lore != null ? lore.stream().map(SERIALIZER::deserialize).collect(Collectors.toList()) : null;
        updateMeta(meta -> meta.lore(comps));
    }

    @Override
    public void setSkullOwner(String playerName) {
        this.skullOwner = playerName;
        this.skullTextureBase64 = null;
        setMaterial(Material.PLAYER_HEAD);
        updateMeta(meta -> {
            if (meta instanceof SkullMeta sm) sm.setOwner(playerName);
        });
    }

    @Override
    public void setSkullTextureBase64(String base64) {
        if (base64 == null) return;
        this.skullTextureBase64 = base64;
        this.skullOwner = null;
        setMaterial(Material.PLAYER_HEAD);

        CompletableFuture.runAsync(() -> this.cachedProfile = PROFILE_CACHE.computeIfAbsent(base64, this::createProfile)).thenRun(() -> updateMeta(meta -> {
            if (meta instanceof SkullMeta sm) applySkullProfile(sm);
        }));
    }

    private Object createProfile(String base64) {
        UUID id = new UUID(base64.hashCode(), base64.hashCode());
        if (IS_PAPER) {
            var profile = Bukkit.createProfile(id, "CustomHead");
            profile.setProperty(new ProfileProperty("textures", base64));
            return profile;
        } else {
            GameProfile profile = new GameProfile(id, "CustomHead");
            profile.getProperties().put("textures", new Property("textures", base64));
            return profile;
        }
    }

    private void applySkullProfile(SkullMeta meta) {
        if (cachedProfile == null) return;
        if (IS_PAPER && cachedProfile instanceof com.destroystokyo.paper.profile.PlayerProfile pp) {
            meta.setPlayerProfile(pp);
        } else {
            try {
                if (skullProfileField == null) {
                    skullProfileField = meta.getClass().getDeclaredField("profile");
                    skullProfileField.setAccessible(true);
                }
                skullProfileField.set(meta, cachedProfile);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public ItemStack buildItemStack() {
        return internalStack;
    }

    public CompletableFuture<ItemStack> buildItemStackAsync() {
        return CompletableFuture.completedFuture(buildItemStack());
    }

    @Override
    public Component getDisplayName() {
        if (displayName != null) return displayName;
        return rawDisplayName != null ? (this.displayName = SERIALIZER.deserialize(rawDisplayName)) : null;
    }

    @Override
    public List<Component> getLore() {
        if (lore != null) return lore;
        return loreStrings != null ? (this.lore = loreStrings.stream().map(SERIALIZER::deserialize).collect(Collectors.toList())) : Collections.emptyList();
    }

    @Override
    public void addEnchant(Enchantment e, int l) { updateMeta(meta -> meta.addEnchant(e, l, true)); }
    @Override
    public void removeEnchant(Enchantment e) { updateMeta(meta -> meta.removeEnchant(e)); }
    @Override
    public void addItemFlag(ItemFlag f) { updateMeta(meta -> meta.addItemFlags(f)); }
    @Override
    public void removeItemFlag(ItemFlag f) { updateMeta(meta -> meta.removeItemFlags(f)); }
    @Override
    public void setCustomModelData(Integer d) { updateMeta(meta -> meta.setCustomModelData(d)); }
    @Override
    public void setColor(Color c) { updateMeta(meta -> { if (meta instanceof LeatherArmorMeta lm) lm.setColor(c); }); }
    @Override
    public void addAttribute(Attribute a, AttributeModifier m) { updateMeta(meta -> meta.addAttributeModifier(a, m)); }
    @Override
    public void removeAttribute(Attribute a) { updateMeta(meta -> meta.removeAttributeModifier(a)); }

    @Override
    public int getEnchantLevel(Enchantment e) { return internalStack.getEnchantmentLevel(e); }
    @Override
    public Map<Enchantment, Integer> getEnchants() { return internalStack.getEnchantments(); }
    @Override
    public boolean hasItemFlag(ItemFlag f) { org.bukkit.inventory.meta.ItemMeta m = internalStack.getItemMeta(); return m != null && m.hasItemFlag(f); }
    @Override
    public Set<ItemFlag> getItemFlags() { org.bukkit.inventory.meta.ItemMeta m = internalStack.getItemMeta(); return m != null ? m.getItemFlags() : Collections.emptySet(); }
    @Override
    public Map<Attribute, Set<AttributeModifier>> getAttributes() { org.bukkit.inventory.meta.ItemMeta m = internalStack.getItemMeta(); return m != null && m.getAttributeModifiers() != null ? (Map) m.getAttributeModifiers().asMap() : Collections.emptyMap(); }
    @Override public Material getMaterial() { return material; }
    @Override public int getAmount() { return amount; }
    @Override public Color getColor() { org.bukkit.inventory.meta.ItemMeta m = internalStack.getItemMeta(); return m instanceof LeatherArmorMeta lm ? lm.getColor() : null; }
    @Override public Integer getCustomModelData() { org.bukkit.inventory.meta.ItemMeta m = internalStack.getItemMeta(); return m != null && m.hasCustomModelData() ? m.getCustomModelData() : null; }
    @Override public String getRawDisplayName() { if (rawDisplayName != null) return rawDisplayName; Component d = getDisplayName(); return d != null ? SERIALIZER.serialize(d) : null; }
    @Override public List<String> getLoreStrings() { if (loreStrings != null) return loreStrings; List<Component> l = getLore(); return l.stream().map(SERIALIZER::serialize).collect(Collectors.toList()); }
    @Override public boolean hasCustomSkullTexture() { return skullTextureBase64 != null; }

    @Override
    public ItemMinecraft clone() {
        try {
            ItemMinecraft cloned = (ItemMinecraft) super.clone();
            cloned.internalStack = internalStack.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}