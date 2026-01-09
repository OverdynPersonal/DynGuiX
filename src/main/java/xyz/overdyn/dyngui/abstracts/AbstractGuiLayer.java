package xyz.overdyn.dyngui.abstracts;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import xyz.overdyn.dyngui.items.GuiItem;
import xyz.overdyn.dyngui.items.ItemWrapper;
import xyz.overdyn.dyngui.policy.GuiPolicy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Abstract GUI layer with dynamic item support and optional auto-update.
 *
 * <p>Supports:</p>
 * <ul>
 *     <li>Registration of {@link ItemWrapper}-based GUI elements</li>
 *     <li>Dynamic updates of items via {@link #updateAll}</li>
 *     <li>Auto-refresh loop via {@link #enableAutoUpdate}</li>
 *     <li>Slot handlers and inventory clearing on unregister</li>
 * </ul>
 */
public abstract class AbstractGuiLayer extends AbstractGuiController {

    /** List of all registered GUI items for this layer. */
    private final List<GuiItem> items = new ArrayList<>();

    /** Scheduled task that periodically updates GUI items when auto-update is enabled. */
    private BukkitTask taskUpdate;

    /** Flag indicating whether auto-update is currently enabled. */
    private boolean updatesEnabled;

    /** Flag used internally to indicate a bulk update in progress. */
    private boolean updating;

    {
        onClose(event -> disableAutoUpdate());
    }

    /**
     * Constructs a GUI layer with a specific inventory type.
     *
     * @param type   The type of inventory to use (e.g., CHEST, DISPENSER)
     * @param title  The display title of the GUI
     * @param policy The GUI policy defining interaction rules
     */
    public AbstractGuiLayer(InventoryType type, @NotNull Component title, GuiPolicy policy) {
        super(type, title, policy);
    }

    /**
     * Constructs a GUI layer with the default chest size (54 slots).
     *
     * @param title  The display title of the GUI
     * @param policy The GUI policy defining interaction rules
     */
    public AbstractGuiLayer(@NotNull Component title, GuiPolicy policy) {
        super(title, policy);
    }

    /**
     * Constructs a GUI layer with a custom inventory size.
     *
     * @param size   Number of slots in the inventory (must be multiple of 9)
     * @param title  The display title of the GUI
     * @param policy The GUI policy defining interaction rules
     */
    public AbstractGuiLayer(int size, @NotNull Component title, GuiPolicy policy) {
        super(size, title, policy);
    }

    /**
     * Opens the GUI for a player.
     *
     * <p>This method will automatically update all registered items before showing
     * the inventory to the player. It ensures that items are correctly rendered
     * on the first open.</p>
     *
     * @param player The player who will view the GUI
     */
    @Override
    public final void open(@NotNull HumanEntity player) {
        updateAll(player, true);
        super.open(player);
    }

    /**
     * Enables automatic updates of all registered GUI items at a fixed interval.
     *
     * <p>This method starts a scheduled task that will call {@link #updateAll(HumanEntity)}
     * for the current viewer of the GUI at every {@code periodTicks} ticks.</p>
     * <p>If the player closes the GUI or goes offline, the auto-update task is cancelled
     * automatically.</p>
     *
     * @param periodTicks Interval in server ticks between each update (20 ticks = 1 second)
     */
    public void enableAutoUpdate(long periodTicks) {
        updatesEnabled = true;

        if (taskUpdate != null) taskUpdate.cancel();

        taskUpdate = scheduler.runTask(() -> {
            if (!updatesEnabled || !isOpen() || getViewer() == null || !getViewer().isOnline()) {
                disableAutoUpdate();
                return;
            }

            updating = true;
            updateAll(getViewer());
            updating = false;

        }, periodTicks, periodTicks);
    }

    /**
     * Disables any active auto-update loop.
     *
     * <p>Cancels the scheduled task and sets {@link #updatesEnabled} to false.</p>
     */
    public void disableAutoUpdate() {
        updatesEnabled = false;
        if (taskUpdate != null) {
            taskUpdate.cancel();
            taskUpdate = null;
        }
    }

    /**
     * Returns the list of all GUI items currently registered in this layer.
     *
     * <p>The returned collection represents the internal state of the GUI layer
     * and contains all {@link GuiItem} instances that are currently bound to
     * one or more inventory slots.</p>
     *
     * <p><b>Important:</b> the returned list is backed by the internal storage.
     * Modifying it directly may lead to inconsistent GUI state. External code
     * should treat this list as read-only unless explicitly managing the GUI
     * lifecycle.</p>
     *
     * @return the list of registered {@link GuiItem}s for this GUI layer
     */
    public List<GuiItem> getItems() {
        return items;
    }

    /**
     * Registers a GUI item using hard replacement strategy.
     *
     * <p>If any existing {@link GuiItem} occupies one or more of the same slots,
     * it will be fully unregistered: its inventory slots will be cleared,
     * click handlers removed, and the item itself removed from the GUI.</p>
     *
     * <p>The provided item is then rendered and bound to all of its declared slots.</p>
     *
     * @param item the {@link GuiItem} to register
     */
    public void registerItem(@NotNull GuiItem item) {
        if (item.getSlots().isEmpty()) return;
        var pendingSlots = new HashSet<>(item.getSlots());

        for (int slot : pendingSlots) {
            var existing = getItem(slot);

            if (existing != null) {
                pendingSlots.removeAll(existing.getSlots());
                unregisterItem(existing);
            } else {
                getInventory().clear(slot);
            }
        }

        var itemStack = item.render(getViewer());

        setSlotHandlers(item.getSlots(), item::handleClick);
        items.add(item);

        for (int slot : item.getSlots()) {
            getInventory().setItem(slot, itemStack);
        }
    }

    /**
     * Registers a GUI item using overlay placement strategy.
     *
     * <p>If one or more target slots are already occupied, the existing {@link GuiItem}
     * is partially overridden: only the conflicting slots are removed from it.
     * Other slots owned by the existing item remain untouched.</p>
     *
     * <p>Click handlers and inventory contents are updated accordingly.
     * If an existing item loses all of its slots as a result of the overlay,
     * it is fully unregistered.</p>
     *
     * <p>This strategy allows layered GUI composition where higher-priority
     * items may override specific slots without destroying unrelated layout.</p>
     *
     * @param item the {@link GuiItem} to register
     */

    public void registerItemOverlay(@NotNull GuiItem item) {
        if (item.getSlots().isEmpty()) return;

        for (int slot : item.getSlots()) {
            unregisterSlotOnly(slot);
        }

        var itemStack = item.render(getViewer());

        setSlotHandlers(item.getSlots(), item::handleClick);
        items.add(item);

        for (int slot : item.getSlots()) {
            getInventory().setItem(slot, itemStack);
        }
    }

    /**
     * Unregisters the GUI item only from a specific slot, without affecting its other slots.
     *
     * <p>
     * The GuiItem remains registered on all other slots it occupies. Only the specified
     * slot is cleared and unregistered from click handlers.
     * </p>
     *
     * @param slot The target inventory slot to unregister
     */
    public void unregisterSlotOnly(int slot) {
        GuiItem item = getItem(slot);
        if (item == null) return;

        item.getSlots().remove(slot);
        removeSlotHandler(slot);
        getInventory().clear(slot);

        if (item.getSlots().isEmpty()) {
            items.remove(item);
        }
    }

    /**
     * Unregisters all GUI items from this layer.
     *
     * <p>This method completely clears the GUI state:
     * all registered {@link GuiItem}s are removed, their occupied inventory slots
     * are cleared, and all associated click handlers are unbound.</p>
     *
     * <p>After invocation, the GUI layer contains no items and behaves as a freshly
     * initialized layer.</p>
     *
     * <p>This method is typically used when rebuilding the GUI layout,
     * switching pages, or performing a full reset.</p>
     */
    public void unregisterAllItems() {
        for (GuiItem item : items) {
            removeSlotHandlers(item.getSlots());
            item.getSlots().forEach(getInventory()::clear);
        }

        items.clear();
    }

    /**
     * Unregisters the GUI item occupying a given slot.
     *
     * @param slot The inventory slot index
     */
    public void unregisterItem(int slot) {
        GuiItem item = getItem(slot);
        if (item != null) unregisterItem(item);
    }

    /**
     * Unregisters a specific GUI item from the layer.
     *
     * <p>Removes click handlers, clears slots from the inventory, and removes
     * the item from the internal registry.</p>
     *
     * @param item The {@link GuiItem} to remove
     */
    public void unregisterItem(@NotNull GuiItem item) {
        if (!items.contains(item)) return;
        items.remove(item);
        removeSlotHandlers(item.getSlots());
        item.getSlots().forEach(getInventory()::clear);
    }

    /**
     * Updates all registered GUI items for the specified player.
     *
     * <p>Each item will be re-rendered and its slots updated in the inventory.
     * Only items marked for updating or first-time renders will be updated.</p>
     *
     * @param player The player context for rendering
     */
    public void updateAll(@NotNull HumanEntity player) {
        updateAll(player, false);
    }

    /**
     * Internal method to update all items, optionally forcing first-time render.
     *
     * @param player The player context for rendering
     * @param first  True if this is the first render (forces update even if item.isUpdate() is false)
     */
    private void updateAll(@NotNull HumanEntity player, boolean first) {
        for (var item : items) {
            if (!item.isUpdate() && !first) continue;
            var itemStack = item.render((OfflinePlayer) player);
            for (int slot : item.getSlots()) {
                getInventory().setItem(slot, itemStack);
            }
        }
    }

    /**
     * Updates a single slot's item visually for the current viewer.
     *
     * @param slot Slot index to update
     */
    public void updateSlot(int slot) {
        if (getViewer() == null) return;
        GuiItem item = getItem(slot);
        if (item == null) return;
        item.render(getViewer());
        if (!item.getSlots().isEmpty()) {
            var itemStack = item.baseItemStack();
            for (int s : item.getSlots()) {
                getInventory().setItem(s, itemStack);
            }
        }
    }

    /**
     * Retrieves the {@link GuiItem} occupying a given slot.
     *
     * @param slot Target slot index
     * @return The {@link GuiItem} occupying the slot, or null if none
     */
    public GuiItem getItem(int slot) {
        for (GuiItem item : items) {
            if (item.getSlots().contains(slot)) return item;
        }
        return null;
    }

    /**
     * Checks if a bulk update operation is currently in progress.
     *
     * @return True if the GUI is currently updating all items
     */
    public boolean isUpdating() {
        return updating;
    }
}
