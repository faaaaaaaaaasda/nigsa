package com.wincors.smp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Admin vanish: hidden from every player without wincors.admin, silent join/quit,
 * mobs ignore you, no item pickup, no pressure plates. Saved across restarts.
 */
public final class VanishManager implements Listener {
    private final WincorsSMP plugin;

    public VanishManager(WincorsSMP plugin) { this.plugin = plugin; }

    public boolean isVanished(Player p) {
        return plugin.data().get(p.getUniqueId()).vanished;
    }

    public void setVanished(Player p, boolean vanish) {
        DataManager.PlayerData d = plugin.data().get(p.getUniqueId());
        boolean was = d.vanished;
        d.vanished = vanish;
        plugin.data().markDirty();
        if (vanish) apply(p); else unapply(p);
        if (was != vanish && plugin.getConfig().getBoolean("vanish.fake-messages", true)) {
            String msg = Msg.t(vanish ? "vanish-fake-leave" : "vanish-fake-join", "player", p.getName());
            for (Player o : Bukkit.getOnlinePlayers()) {
                if (!o.equals(p) && !o.hasPermission("wincors.admin")) o.sendMessage(Text.c(msg));
            }
        }
        p.sendActionBar(Text.c(Msg.t(vanish ? "vanish-on" : "vanish-off")));
    }

    private void apply(Player p) {
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o.equals(p)) continue;
            if (o.hasPermission("wincors.admin")) o.showPlayer(plugin, p);
            else o.hidePlayer(plugin, p);
        }
        p.setCollidable(false);
        p.setSleepingIgnored(true);
    }

    private void unapply(Player p) {
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (!o.equals(p)) o.showPlayer(plugin, p);
        }
        p.setCollidable(true);
        p.setSleepingIgnored(false);
    }

    /** Called on plugin disable so nobody stays hidden after a reload. */
    public void shutdown() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isVanished(p)) unapply(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        boolean vanished = isVanished(p);
        if (vanished) e.joinMessage(null);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            // Hide currently vanished admins from the joiner (unless the joiner is an admin)
            if (!p.hasPermission("wincors.admin")) {
                for (Player o : Bukkit.getOnlinePlayers()) {
                    if (!o.equals(p) && isVanished(o)) p.hidePlayer(plugin, o);
                }
            }
            if (vanished) {
                apply(p);
                Text.msg(p, Msg.t("vanish-join"));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent e) {
        if (isVanished(e.getPlayer())) e.quitMessage(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent e) {
        if (e.getTarget() instanceof Player p && isVanished(p)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && isVanished(p)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPhysical(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL && isVanished(e.getPlayer())) e.setCancelled(true);
    }
}
