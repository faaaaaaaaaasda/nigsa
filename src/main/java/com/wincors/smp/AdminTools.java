package com.wincors.smp;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Freeze and mute state (in memory, cleared on restart). */
public final class AdminTools implements Listener {
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> muted = new HashSet<>();

    /** @return true if now frozen */
    public boolean toggleFreeze(Player p) {
        if (frozen.remove(p.getUniqueId())) return false;
        frozen.add(p.getUniqueId());
        return true;
    }

    /** @return true if now muted */
    public boolean toggleMute(Player p) {
        if (muted.remove(p.getUniqueId())) return false;
        muted.add(p.getUniqueId());
        return true;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!frozen.contains(e.getPlayer().getUniqueId())) return;
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location l = from.clone();
            l.setYaw(to.getYaw());
            l.setPitch(to.getPitch());
            e.setTo(l);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        if (muted.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            Text.msg(e.getPlayer(), Msg.t("muted"));
        }
    }
}
