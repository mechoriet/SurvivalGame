package com.jabyftw.sgames.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author nisovin - http://forums.bukkit.org/threads/icon-menu.108342/
 * @editor Rafael
 */
@SuppressWarnings("UnusedDeclaration")
public class IconMenu implements Listener {

    private final Plugin plugin;
    private final String name;
    private final int size, page;
    private final IconMenuEventHandler handler;
    private ItemStack[] Icons;

    public IconMenu(Plugin plugin, String name, int size, int page, IconMenuEventHandler handler) {
        this.plugin = plugin;
        this.name = name.replaceAll("&", "§");
        this.size = size;
        this.page = page;
        this.handler = handler;
        this.Icons = new ItemStack[size];
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public int getPage() {
        return page;
    }

    public IconMenu setOption(int position, ItemStack icon) {
        Icons[position] = icon;
        return this;
    }

    public void destroy() {
        HandlerList.unregisterAll(this);
    }

    public void openInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(player, size, name);
        for(int i = 0; i < Icons.length; i++) {
            inventory.setItem(i, Icons[i]);
        }
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onInventoryClick(InventoryClickEvent e) {
        if(e.getInventory().getTitle().equals(name)) {
            e.setCancelled(true);
            if(e.getRawSlot() >= 0 && e.getRawSlot() < size && Icons[e.getRawSlot()] != null) {
                InventoryMenuClickEvent event = new InventoryMenuClickEvent((Player) e.getWhoClicked(), e.getSlot(), page, e.getCurrentItem());
                handler.onInventoryClick(event);
                if(event.isClosing()) {
                    final Player player = (Player) e.getWhoClicked();
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            player.closeInventory();
                        }
                    }.runTaskLater(plugin, 1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onInventoryClose(InventoryCloseEvent e) {
        if(e.getInventory().getTitle().equals(name)) {
            handler.onInventoryClose(new InventoryMenuCloseEvent((Player) e.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onInventoryMove(InventoryMoveItemEvent e) {
        if(e.getInitiator().getTitle().equals(name)) {
            e.setCancelled(true);
        }
    }

    public interface IconMenuEventHandler {

        public void onInventoryClick(InventoryMenuClickEvent e);

        public void onInventoryClose(InventoryMenuCloseEvent e);
    }

    public class InventoryMenuClickEvent {

        private final Player player;
        private final int slot, page;
        private final ItemStack item;

        private boolean closing = false;

        public InventoryMenuClickEvent(Player player, int slot, int page, ItemStack item) {
            this.player = player;
            this.slot = slot;
            this.page = page;
            this.item = item;
        }

        public Player getPlayer() {
            return player;
        }

        public int getSlot() {
            return slot;
        }

        public int getPage() {
            return page;
        }

        public ItemStack getItem() {
            return item;
        }

        public boolean isClosing() {
            return closing;
        }

        public void setClosing(boolean closing) {
            this.closing = closing;
        }
    }

    public class InventoryMenuCloseEvent {

        private final Player player;

        public InventoryMenuCloseEvent(Player player) {
            this.player = player;
        }

        public Player getPlayer() {
            return player;
        }
    }
}
