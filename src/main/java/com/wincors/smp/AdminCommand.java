package com.wincors.smp;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /wincors <...> and /vanish. 58 admin actions. */
public final class AdminCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBS = List.of(
        "gui", "help", "info", "top", "stats", "players", "ping", "end", "origin", "mana", "crystal", "crystalall",
        "weapon", "mace", "combat", "cooldown", "egg", "resetplayer", "vanish", "heal", "feed", "fly", "god",
        "gamemode", "speed", "tp", "tphere", "freeze", "mute", "clearinv", "invsee", "enderchest", "kill", "smite",
        "broadcast", "config", "backup", "save", "reload");

    private final WincorsSMP plugin;

    public AdminCommand(WincorsSMP plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!s.hasPermission("wincors.admin")) {
            Text.msg(s, "&cYou don't have permission.");
            return true;
        }
        if (c.getName().equalsIgnoreCase("vanish")) {
            String[] n = new String[a.length + 1];
            n[0] = "vanish";
            System.arraycopy(a, 0, n, 1, a.length);
            a = n;
        }
        if (a.length == 0) {
            if (s instanceof Player p) plugin.gui().openAdmin(p); else help(s);
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "gui" -> { if (s instanceof Player p) plugin.gui().openAdmin(p); else Text.msg(s, "Players only."); }
            case "help" -> help(s);
            case "info" -> info(s, a);
            case "top" -> top(s);
            case "stats" -> stats(s);
            case "players" -> players(s);
            case "ping" -> { Player t = target(s, a, 1); if (t != null) Text.msg(s, t.getName() + "'s ping: &f" + t.getPing() + "ms"); }
            case "end" -> end(s, a);
            case "origin" -> origin(s, a);
            case "mana" -> mana(s, a);
            case "crystal" -> crystal(s, a);
            case "crystalall" -> crystalAll(s, a);
            case "weapon" -> weapon(s, a);
            case "mace" -> mace(s, a);
            case "combat" -> combat(s, a);
            case "cooldown" -> cooldown(s, a);
            case "egg" -> egg(s, a);
            case "resetplayer" -> {
                Player t = target(s, a, 1);
                if (t != null) { plugin.mana().resetPlayer(t); Text.msg(s, "Reset " + t.getName() + "."); }
            }
            case "vanish", "v" -> vanish(s, a);
            case "heal" -> {
                Player t = targetOrSelf(s, a, 1);
                if (t != null) {
                    t.setHealth(t.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                    t.setFireTicks(0);
                    Text.msg(s, "Healed " + t.getName() + ".");
                }
            }
            case "feed" -> {
                Player t = targetOrSelf(s, a, 1);
                if (t != null) { t.setFoodLevel(20); t.setSaturation(20f); Text.msg(s, "Fed " + t.getName() + "."); }
            }
            case "fly" -> {
                Player t = targetOrSelf(s, a, 1);
                if (t != null) {
                    boolean on = !t.getAllowFlight();
                    t.setAllowFlight(on);
                    if (on) t.setFlying(true);
                    Text.msg(s, "Flight for " + t.getName() + ": " + (on ? "&aON" : "&cOFF"));
                }
            }
            case "god" -> {
                Player t = targetOrSelf(s, a, 1);
                if (t != null) {
                    boolean on = !t.isInvulnerable();
                    t.setInvulnerable(on);
                    Text.msg(s, "God mode for " + t.getName() + ": " + (on ? "&aON" : "&cOFF"));
                }
            }
            case "gamemode", "gm" -> gamemode(s, a);
            case "speed" -> speed(s, a);
            case "tp" -> {
                Player t = target(s, a, 1);
                if (t != null && s instanceof Player p) { p.teleport(t); Text.msg(s, "Teleported to " + t.getName() + "."); }
                else if (t != null) Text.msg(s, "Players only.");
            }
            case "tphere" -> {
                Player t = target(s, a, 1);
                if (t != null && s instanceof Player p) { t.teleport(p); Text.msg(s, "Teleported " + t.getName() + " to you."); }
                else if (t != null) Text.msg(s, "Players only.");
            }
            case "freeze" -> {
                Player t = target(s, a, 1);
                if (t != null) {
                    boolean on = plugin.tools().toggleFreeze(t);
                    Text.msg(s, t.getName() + (on ? " is now &bfrozen" : " is no longer frozen") + "&r.");
                    Text.msg(t, on ? "&cYou have been frozen by an admin." : "&aYou are no longer frozen.");
                }
            }
            case "mute" -> {
                Player t = target(s, a, 1);
                if (t != null) {
                    boolean on = plugin.tools().toggleMute(t);
                    Text.msg(s, t.getName() + (on ? " is now &cmuted" : " is no longer muted") + "&r.");
                }
            }
            case "clearinv" -> {
                Player t = target(s, a, 1);
                if (t != null) {
                    t.getInventory().clear();
                    plugin.gui().ensureMenuItem(t);
                    Text.msg(s, "Cleared " + t.getName() + "'s inventory.");
                }
            }
            case "invsee" -> {
                Player t = target(s, a, 1);
                if (t != null && s instanceof Player p) p.openInventory(t.getInventory());
                else if (t != null) Text.msg(s, "Players only.");
            }
            case "enderchest", "ec" -> {
                Player t = target(s, a, 1);
                if (t != null && s instanceof Player p) p.openInventory(t.getEnderChest());
                else if (t != null) Text.msg(s, "Players only.");
            }
            case "kill" -> {
                Player t = target(s, a, 1);
                if (t != null) { t.setHealth(0.0); Text.msg(s, "Killed " + t.getName() + "."); }
            }
            case "smite" -> {
                Player t = target(s, a, 1);
                if (t != null) { t.getWorld().strikeLightning(t.getLocation()); Text.msg(s, "Smote " + t.getName() + "."); }
            }
            case "broadcast", "bc" -> {
                if (a.length < 2) { Text.msg(s, "&cUsage: /wincors broadcast <message>"); return true; }
                Bukkit.broadcast(Text.c("&8[&6Broadcast&8] &e" + String.join(" ", Arrays.copyOfRange(a, 1, a.length))));
            }
            case "config" -> config(s, a);
            case "backup" -> backup(s);
            case "save" -> { plugin.data().save(); Text.msg(s, "&aData saved."); }
            case "reload" -> {
                plugin.reloadAll();
                Text.msg(s, "&aAll config files reloaded (config, powers, origins, messages, recipe).");
            }
            default -> help(s);
        }
        return true;
    }

    // =================== help ===================

    private void help(CommandSender s) {
        Text.msg(s, "&6Wincors admin &7(run &e/wincors &7in-game for the admin GUI) &8- &758 actions");
        String[] lines = {
            "&6Info: &e/wincors info <p> | top | stats | players | ping <p> | egg [give <p>]",
            "&6Mana/Origins: &e/wincors mana <set|add|take|reset> <p> [n] | origin <set|random|clear|swap|pick> ...",
            "&6Items: &e/wincors crystal <p> [n] | crystalall [n] | weapon <give|reset> <p> | mace <info|reset|give>",
            "&6Combat: &e/wincors combat <clear|status|tag|toggle> ... | cooldown reset <p|all>",
            "&6The End: &e/wincors end <open|close|toggle|status|evacuate>",
            "&6Vanish: &e/vanish [player|list] &7(or /wincors vanish)",
            "&6Player tools: &e/wincors heal|feed|fly|god [p] | gamemode <mode> [p] | speed <1-10|reset> [p]",
            "&6Moderation: &e/wincors tp|tphere|freeze|mute|clearinv|invsee|enderchest|kill|smite <p>",
            "&6Server: &e/wincors broadcast <msg> | config <get|set> <path> [value] | backup | save | reload | resetplayer <p>"
        };
        for (String x : lines) s.sendMessage(Text.c(x));
    }

    // =================== helpers ===================

    private Player target(CommandSender s, String[] a, int idx) {
        if (a.length <= idx) { Text.msg(s, "&cMissing player name."); return null; }
        Player t = Bukkit.getPlayerExact(a[idx]);
        if (t == null) Text.msg(s, "&cPlayer not online: " + a[idx]);
        return t;
    }

    private Player targetOrSelf(CommandSender s, String[] a, int idx) {
        if (a.length > idx) return target(s, a, idx);
        if (s instanceof Player p) return p;
        Text.msg(s, "&cSpecify a player.");
        return null;
    }

    // =================== info ===================

    private void info(CommandSender s, String[] a) {
        Player t = target(s, a, 1);
        if (t == null) return;
        DataManager.PlayerData d = plugin.data().get(t.getUniqueId());
        Text.msg(s, "&6" + t.getName());
        String[] lines = {
            "&7Origin: " + (d.origin == null ? "&cnone" : d.origin.colored()),
            "&7Mana: &b" + d.mana + "/10 &7| Power tier: &6" + plugin.mana().tier(t),
            "&7Weapon claimed: &f" + d.weaponClaimed,
            "&7In combat: &f" + (plugin.combat().isTagged(t) ? plugin.combat().remaining(t) + "s" : "no"),
            "&7Carrying Dragon Egg: &f" + plugin.egg().hasEgg(t),
            "&7Vanished: &f" + d.vanished + " &7| God: &f" + t.isInvulnerable() + " &7| Flying allowed: &f" + t.getAllowFlight(),
            "&7Gamemode: &f" + t.getGameMode() + " &7| Ping: &f" + t.getPing() + "ms"
        };
        for (String x : lines) s.sendMessage(Text.c(x));
    }

    private void top(CommandSender s) {
        List<Map.Entry<UUID, DataManager.PlayerData>> list = new ArrayList<>(plugin.data().all().entrySet());
        list.removeIf(en -> en.getValue().origin == null);
        list.sort((x, y) -> Integer.compare(y.getValue().mana, x.getValue().mana));
        Text.msg(s, "&6Top mana:");
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            Map.Entry<UUID, DataManager.PlayerData> en = list.get(i);
            String n = Bukkit.getOfflinePlayer(en.getKey()).getName();
            if (n == null) n = en.getKey().toString().substring(0, 8);
            s.sendMessage(Text.c("&e" + (i + 1) + ". &f" + n + " &8- " + en.getValue().origin.colored() + " &8- &b" + en.getValue().mana + " mana"));
        }
        if (list.isEmpty()) s.sendMessage(Text.c("&7Nobody has an origin yet."));
    }

    private void stats(CommandSender s) {
        Map<Origin, Integer> counts = new EnumMap<>(Origin.class);
        int total = 0, maxed = 0, weapons = 0, powerless = 0;
        for (DataManager.PlayerData d : plugin.data().all().values()) {
            total++;
            if (d.origin == null) { powerless++; continue; }
            counts.merge(d.origin, 1, Integer::sum);
            if (d.mana >= ManaManager.MAX) maxed++;
            if (d.weaponClaimed) weapons++;
        }
        Text.msg(s, "&6Server stats &7(" + total + " known players)");
        for (Origin o : Origin.values()) s.sendMessage(Text.c(" " + o.colored() + "&7: &f" + counts.getOrDefault(o, 0)));
        s.sendMessage(Text.c("&7Powerless: &f" + powerless + " &7| At max mana: &f" + maxed + " &7| Weapons claimed: &f" + weapons));
        s.sendMessage(Text.c("&7The End: " + (plugin.end().isOpen() ? "&aOPEN" : "&cCLOSED") + " &7| Mace: " + (plugin.data().getMaceId() != null ? "&aexists" : "&7not crafted")));
    }

    private void players(CommandSender s) {
        Text.msg(s, "&6Online players:");
        for (Player p : Bukkit.getOnlinePlayers()) {
            DataManager.PlayerData d = plugin.data().get(p.getUniqueId());
            s.sendMessage(Text.c(" &f" + p.getName() + " &8- " + (d.origin == null ? "&cno origin" : d.origin.colored() + " &b" + d.mana + "/10")
                + (d.vanished ? " &b[V]" : "") + (plugin.combat().isTagged(p) ? " &c[combat]" : "")));
        }
    }

    // =================== vanish ===================

    private void vanish(CommandSender s, String[] a) {
        if (a.length > 1 && a[1].equalsIgnoreCase("list")) {
            Text.msg(s, "&bVanished players:");
            boolean any = false;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.vanish().isVanished(p)) { s.sendMessage(Text.c(" &f" + p.getName())); any = true; }
            }
            if (!any) s.sendMessage(Text.c(" &7nobody"));
            return;
        }
        Player t = targetOrSelf(s, a, 1);
        if (t == null) return;
        boolean now = !plugin.vanish().isVanished(t);
        plugin.vanish().setVanished(t, now);
        Text.msg(s, t.getName() + " is now " + (now ? "&bvanished" : "&avisible") + "&r.");
        if (!t.equals(s)) Text.msg(t, "You are now " + (now ? "&bvanished" : "&avisible") + "&r.");
    }

    // =================== end ===================

    private void end(CommandSender s, String[] a) {
        String usage = "&cUsage: /wincors end <open|close|toggle|status|evacuate>";
        if (a.length < 2) { Text.msg(s, usage); return; }
        switch (a[1].toLowerCase()) {
            case "open" -> plugin.end().setOpen(true, s);
            case "close" -> plugin.end().setOpen(false, s);
            case "toggle" -> plugin.end().setOpen(!plugin.end().isOpen(), s);
            case "status" -> Text.msg(s, "The End is " + (plugin.end().isOpen() ? "&aOPEN" : "&cCLOSED"));
            case "evacuate" -> Text.msg(s, "Moved " + plugin.end().evacuate() + " player(s) out of the End.");
            default -> Text.msg(s, usage);
        }
    }

    // =================== origin / mana / crystals ===================

    private void origin(CommandSender s, String[] a) {
        String usage = "&cUsage: /wincors origin <set|random|clear> <player> [origin]  |  origin swap <p1> <p2>";
        if (a.length < 3) { Text.msg(s, usage); return; }
        if (a[1].equalsIgnoreCase("swap")) {
            Player p1 = target(s, a, 2);
            Player p2 = target(s, a, 3);
            if (p1 == null || p2 == null) return;
            DataManager.PlayerData d1 = plugin.data().get(p1.getUniqueId());
            DataManager.PlayerData d2 = plugin.data().get(p2.getUniqueId());
            Origin o = d1.origin; int m = d1.mana; boolean w = d1.weaponClaimed;
            d1.origin = d2.origin; d1.mana = d2.mana; d1.weaponClaimed = d2.weaponClaimed;
            d2.origin = o; d2.mana = m; d2.weaponClaimed = w;
            plugin.data().markDirty();
            Text.msg(s, "Swapped origins/mana of " + p1.getName() + " and " + p2.getName() + ".");
            return;
        }
        Player t = target(s, a, 2);
        if (t == null) return;
        switch (a[1].toLowerCase()) {
            case "set" -> {
                Origin o = a.length > 3 ? Origin.parse(a[3]) : null;
                if (o == null) { Text.msg(s, "&cUnknown origin."); return; }
                plugin.mana().setOrigin(t, o);
                Text.msg(s, "Set " + t.getName() + "'s origin to " + o.colored());
            }
            case "pick" -> {
                plugin.data().get(t.getUniqueId()).pendingChoice = true;
                plugin.data().markDirty();
                plugin.gui().openChoose(t);
                Text.msg(s, "Opened the origin picker for " + t.getName() + ".");
            }
            case "random" -> {
                Origin o = plugin.mana().randomOrigin();
                plugin.mana().setOrigin(t, o);
                Text.msg(s, "Gave " + t.getName() + " a random origin: " + o.colored());
            }
            case "clear" -> {
                plugin.mana().clearOrigin(t);
                Text.msg(s, "Cleared " + t.getName() + "'s origin.");
            }
            default -> Text.msg(s, usage);
        }
    }

    private void mana(CommandSender s, String[] a) {
        String usage = "&cUsage: /wincors mana <set|add|take|reset> <player> [amount]";
        if (a.length < 3) { Text.msg(s, usage); return; }
        Player t = target(s, a, 2);
        if (t == null) return;
        int cur = plugin.data().get(t.getUniqueId()).mana;
        String sub = a[1].toLowerCase();
        if (sub.equals("reset")) {
            plugin.mana().setMana(t, plugin.getConfig().getInt("mana.start", 1));
        } else {
            if (a.length < 4) { Text.msg(s, usage); return; }
            int n;
            try { n = Integer.parseInt(a[3]); } catch (NumberFormatException ex) { Text.msg(s, "&cInvalid number."); return; }
            switch (sub) {
                case "set" -> plugin.mana().setMana(t, n);
                case "add" -> plugin.mana().setMana(t, cur + n);
                case "take" -> plugin.mana().setMana(t, cur - n);
                default -> { Text.msg(s, usage); return; }
            }
        }
        Text.msg(s, t.getName() + " now has " + plugin.data().get(t.getUniqueId()).mana + " mana.");
    }

    private int amount(CommandSender s, String[] a, int idx) {
        if (a.length <= idx) return 1;
        try { return Math.max(1, Math.min(64, Integer.parseInt(a[idx]))); }
        catch (NumberFormatException ex) { Text.msg(s, "&cInvalid number."); return -1; }
    }

    private void give(Player t, ItemStack it) {
        for (ItemStack left : t.getInventory().addItem(it).values()) t.getWorld().dropItemNaturally(t.getLocation(), left);
    }

    private void crystal(CommandSender s, String[] a) {
        Player t = target(s, a, 1);
        if (t == null) return;
        int n = amount(s, a, 2);
        if (n < 0) return;
        give(t, plugin.mana().createCrystal(n));
        Text.msg(s, "Gave " + n + " Mana Crystal(s) to " + t.getName());
    }

    private void crystalAll(CommandSender s, String[] a) {
        int n = amount(s, a, 1);
        if (n < 0) return;
        for (Player p : Bukkit.getOnlinePlayers()) give(p, plugin.mana().createCrystal(n));
        Text.msg(s, "Gave " + n + " Mana Crystal(s) to everyone online.");
    }

    // =================== weapon / mace / egg ===================

    private void weapon(CommandSender s, String[] a) {
        if (a.length < 3) { Text.msg(s, "&cUsage: /wincors weapon <give|reset> <player>"); return; }
        Player t = target(s, a, 2);
        if (t == null) return;
        DataManager.PlayerData d = plugin.data().get(t.getUniqueId());
        if (a[1].equalsIgnoreCase("give")) {
            if (d.origin == null) { Text.msg(s, "&cThat player has no origin."); return; }
            plugin.weapons().give(t);
            Text.msg(s, "Gave weapon to " + t.getName());
        } else if (a[1].equalsIgnoreCase("reset")) {
            d.weaponClaimed = false;
            plugin.data().markDirty();
            plugin.mana().checkWeapon(t, d);
            Text.msg(s, "Reset weapon claim for " + t.getName() + " (they get a new one if at max mana).");
        }
    }

    private void mace(CommandSender s, String[] a) {
        String sub = a.length > 1 ? a[1].toLowerCase() : "info";
        switch (sub) {
            case "reset" -> {
                plugin.data().setMaceId(null);
                Text.msg(s, "&aMace registry reset. Old maces will be removed; a new one can be crafted.");
            }
            case "give" -> {
                Player t = target(s, a, 2);
                if (t == null) return;
                ItemStack m = plugin.restrictions().createMace();
                if (m == null) { Text.msg(s, "&cA mace already exists. Use /wincors mace reset first."); return; }
                give(t, m);
                Text.msg(s, "Gave the one mace to " + t.getName());
            }
            default -> {
                if (plugin.data().getMaceId() == null) { Text.msg(s, "No mace has been crafted yet."); return; }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getInventory().contains(Material.MACE)) {
                        Text.msg(s, "The mace is held by &e" + p.getName());
                        return;
                    }
                }
                Text.msg(s, "The mace exists, but no online player is holding it.");
            }
        }
    }

    private void egg(CommandSender s, String[] a) {
        if (a.length > 1 && a[1].equalsIgnoreCase("give")) {
            Player t = target(s, a, 2);
            if (t == null) return;
            give(t, new ItemStack(Material.DRAGON_EGG));
            Text.msg(s, "Gave a Dragon Egg to " + t.getName());
            return;
        }
        boolean any = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.egg().hasEgg(p)) { Text.msg(s, "&5The Dragon Egg is carried by &f" + p.getName()); any = true; }
        }
        if (!any) Text.msg(s, "&7No online player is carrying the Dragon Egg.");
    }

    // =================== combat / cooldown ===================

    private void combat(CommandSender s, String[] a) {
        if (a.length >= 2 && a[1].equalsIgnoreCase("toggle")) {
            boolean on = !plugin.getConfig().getBoolean("combat.enabled", true);
            plugin.getConfig().set("combat.enabled", on);
            plugin.saveConfig();
            if (!on) for (Player p : Bukkit.getOnlinePlayers()) plugin.combat().clear(p);
            Text.msg(s, "Combat tagging is now " + (on ? "&aENABLED" : "&cDISABLED") + "&r.");
            return;
        }
        if (a.length < 3) { Text.msg(s, "&cUsage: /wincors combat <clear|status|tag|toggle> <player> [player2]"); return; }
        Player t = target(s, a, 2);
        if (t == null) return;
        switch (a[1].toLowerCase()) {
            case "clear" -> { plugin.combat().clear(t); Text.msg(s, "Cleared combat tag of " + t.getName()); }
            case "status" -> Text.msg(s, t.getName() + (plugin.combat().isTagged(t) ? " is in combat for &c" + plugin.combat().remaining(t) + "s" : " is not in combat."));
            case "tag" -> {
                Player t2 = target(s, a, 3);
                if (t2 == null) return;
                plugin.combat().tag(t, t2);
                Text.msg(s, "Tagged " + t.getName() + " and " + t2.getName());
            }
            default -> Text.msg(s, "&cUsage: /wincors combat <clear|status|tag|toggle> <player> [player2]");
        }
    }

    private void cooldown(CommandSender s, String[] a) {
        if (a.length < 3 || !a[1].equalsIgnoreCase("reset")) { Text.msg(s, "&cUsage: /wincors cooldown reset <player|all>"); return; }
        if (a[2].equalsIgnoreCase("all")) {
            for (Player p : Bukkit.getOnlinePlayers()) { plugin.powers().resetCooldowns(p); plugin.combat().resetPearl(p); }
            Text.msg(s, "Reset cooldowns for everyone online.");
            return;
        }
        Player t = target(s, a, 2);
        if (t != null) { plugin.powers().resetCooldowns(t); plugin.combat().resetPearl(t); Text.msg(s, "Reset cooldowns of " + t.getName()); }
    }

    // =================== player tools ===================

    private void gamemode(CommandSender s, String[] a) {
        if (a.length < 2) { Text.msg(s, "&cUsage: /wincors gamemode <survival|creative|adventure|spectator> [player]"); return; }
        GameMode gm = switch (a[1].toLowerCase()) {
            case "s", "0", "survival" -> GameMode.SURVIVAL;
            case "c", "1", "creative" -> GameMode.CREATIVE;
            case "a", "2", "adventure" -> GameMode.ADVENTURE;
            case "sp", "3", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
        if (gm == null) { Text.msg(s, "&cUnknown gamemode."); return; }
        Player t = targetOrSelf(s, a, 2);
        if (t == null) return;
        t.setGameMode(gm);
        Text.msg(s, t.getName() + "'s gamemode is now &f" + gm.name().toLowerCase() + "&r.");
    }

    private void speed(CommandSender s, String[] a) {
        if (a.length < 2) { Text.msg(s, "&cUsage: /wincors speed <1-10|reset> [player]"); return; }
        Player t = targetOrSelf(s, a, 2);
        if (t == null) return;
        if (a[1].equalsIgnoreCase("reset")) {
            t.setWalkSpeed(0.2f);
            t.setFlySpeed(0.1f);
            Text.msg(s, "Reset speed of " + t.getName() + ".");
            return;
        }
        int n;
        try { n = Integer.parseInt(a[1]); } catch (NumberFormatException ex) { Text.msg(s, "&cInvalid number."); return; }
        n = Math.max(1, Math.min(10, n));
        t.setWalkSpeed(n / 10f);
        t.setFlySpeed(n / 10f);
        Text.msg(s, "Speed of " + t.getName() + " set to &f" + n + "&r.");
    }

    // =================== config / backup ===================

    private void config(CommandSender s, String[] a) {
        String usage = "&cUsage: /wincors config <get|set> <path> [value]";
        if (a.length < 3) { Text.msg(s, usage); return; }
        String path = a[2];
        Object cur = plugin.getConfig().get(path);
        if (a[1].equalsIgnoreCase("get")) {
            if (cur == null) Text.msg(s, "&cNo such config path."); else Text.msg(s, path + " = &f" + cur);
            return;
        }
        if (!a[1].equalsIgnoreCase("set") || a.length < 4) { Text.msg(s, usage); return; }
        if (cur == null || cur instanceof ConfigurationSection || cur instanceof List) {
            Text.msg(s, "&cOnly existing single values can be set (edit lists in config.yml).");
            return;
        }
        String raw = String.join(" ", Arrays.copyOfRange(a, 3, a.length));
        Object val;
        try {
            if (cur instanceof Boolean) val = Boolean.parseBoolean(raw);
            else if (cur instanceof Integer) val = Integer.parseInt(raw);
            else if (cur instanceof Long) val = Long.parseLong(raw);
            else if (cur instanceof Double) val = Double.parseDouble(raw);
            else val = raw;
        } catch (NumberFormatException ex) {
            Text.msg(s, "&cThat value has the wrong type for " + path + ".");
            return;
        }
        plugin.getConfig().set(path, val);
        plugin.saveConfig();
        if (path.startsWith("recipe")) plugin.mana().registerRecipe();
        Text.msg(s, path + " set to &f" + val + "&r.");
    }

    private void backup(CommandSender s) {
        plugin.data().save();
        File src = new File(plugin.getDataFolder(), "data.yml");
        File dir = new File(plugin.getDataFolder(), "backups");
        dir.mkdirs();
        String name = "data-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".yml";
        try {
            Files.copy(src.toPath(), new File(dir, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
            Text.msg(s, "&aBackup saved: plugins/WincorsSMP/backups/" + name);
        } catch (IOException ex) {
            Text.msg(s, "&cBackup failed: " + ex.getMessage());
        }
    }

    // =================== tab complete ===================

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> out = new ArrayList<>();
        if (!s.hasPermission("wincors.admin")) return out;
        if (c.getName().equalsIgnoreCase("vanish")) {
            if (a.length == 1) { out.add("list"); onlinePlayers(out); }
        } else if (a.length == 1) {
            out.addAll(SUBS);
        } else {
            String sub = a[0].toLowerCase();
            if (a.length == 2) {
                switch (sub) {
                    case "end" -> out.addAll(List.of("open", "close", "toggle", "status", "evacuate"));
                    case "origin" -> out.addAll(List.of("set", "random", "clear", "swap", "pick"));
                    case "mana" -> out.addAll(List.of("set", "add", "take", "reset"));
                    case "weapon" -> out.addAll(List.of("give", "reset"));
                    case "mace" -> out.addAll(List.of("info", "reset", "give"));
                    case "combat" -> out.addAll(List.of("clear", "status", "tag", "toggle"));
                    case "cooldown" -> out.add("reset");
                    case "egg" -> out.addAll(List.of("info", "give"));
                    case "vanish", "v" -> { out.add("list"); onlinePlayers(out); }
                    case "gamemode", "gm" -> out.addAll(List.of("survival", "creative", "adventure", "spectator"));
                    case "speed" -> out.addAll(List.of("1", "2", "5", "10", "reset"));
                    case "config" -> out.addAll(List.of("get", "set"));
                    case "info", "ping", "crystal", "resetplayer", "heal", "feed", "fly", "god", "tp", "tphere",
                         "freeze", "mute", "clearinv", "invsee", "enderchest", "ec", "kill", "smite" -> onlinePlayers(out);
                    default -> { }
                }
            } else if (a.length == 3) {
                if (List.of("origin", "mana", "weapon", "combat", "mace", "egg").contains(sub)) onlinePlayers(out);
                if (sub.equals("cooldown")) { out.add("all"); onlinePlayers(out); }
                if (sub.equals("gamemode") || sub.equals("gm") || sub.equals("speed")) onlinePlayers(out);
                if (sub.equals("config")) {
                    for (String k : plugin.getConfig().getKeys(true)) if (!(plugin.getConfig().get(k) instanceof ConfigurationSection)) out.add(k);
                }
            } else if (a.length == 4) {
                if (sub.equals("origin") && a[1].equalsIgnoreCase("set")) for (Origin o : Origin.values()) out.add(o.display());
                if ((sub.equals("origin") && a[1].equalsIgnoreCase("swap")) || (sub.equals("combat") && a[1].equalsIgnoreCase("tag"))) onlinePlayers(out);
            }
        }
        String last = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(last));
        return out;
    }

    private void onlinePlayers(List<String> out) {
        for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
    }
}
