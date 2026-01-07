package xyz.overdyn.dyngui.cache;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import xyz.overdyn.dyngui.SkullCreator;
import xyz.overdyn.dyngui.items.ItemWrapper;

import java.util.HashMap;
import java.util.Map;

public class SkullCache {
    private static final Map<String, ItemWrapper> cache = new HashMap<>();

    public static ItemWrapper get(String base64) {
        return cache.computeIfAbsent(base64, key -> {
            try {
                return new ItemWrapper(SkullCreator.itemFromBase64(key));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to create skull: " + e.getMessage());
                return new ItemWrapper(Material.PLAYER_HEAD);
            }
        }).clone();
    }
}
