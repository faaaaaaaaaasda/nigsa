# Wincors SMP Plugin

Paper 1.21.11 / Java 21. Build: `mvn clean package` (or push to GitHub, the Actions workflow builds it).

## Everything is configurable (plugins/WincorsSMP/ - run /wincors reload after editing)
- config.yml    mana, mana-crystal recipe, combat, restrictions (banned items, one-mace), dragon egg, End,
                movement, vanish, flag numbers, controls (which key does what), item names/lore, HUD on/off
- powers.yml    ALL 100 powers: names, descriptions, tiers, cooldowns, damage, radius, effects, particles, sounds
- origins.yml   origin names, colors, descriptions, GUI icons AND each legendary weapon (material, enchants, on-hit effect)
- messages.yml  every chat/action-bar message + HUD layout, plus a global find/replace for any other text
(The 10 origin keys and the 10 power tiers are fixed; everything inside them is editable.)

## Choosing your origin (config.yml -> origins)
selection-mode: choose  -> new players pick their origin in a menu (random = old behaviour)
allow-change: true      -> players may switch later (/origin choose or the main menu)
max-players-per-origin  -> cap how many players can share one origin (0 = unlimited)
Players who lose their power pick a new origin when they next gain mana. Admins: /wincors origin pick <player>.

## Ender pearls (config.yml -> ender-pearl)
Usable normally with a 5 minute cooldown. Throwing is blocked while in combat (block-in-pvp), but a pearl thrown before the fight still lands (cancel-landing-in-pvp: false).
The cooldown shows on the pearl item itself. /wincors cooldown reset <player|all> also clears it.

## Visual effects (config.yml -> visuals)
Powers use animated, colour-gradient particles: rotating magic circles, expanding shockwaves, double helixes, glowing shells, strike beams, dash ribbons and impact bursts.
Extra switches: visuals.colored-dust and visuals.magic-circles.
visuals.enabled turns them off; visuals.density scales the amount (0.5 = lighter). Each power uses its own
particle from powers.yml (the `particle` key).

## Keybinds (set these once in Minecraft: Options > Controls > Key Binds)
A server can't read raw keys, only actions, so rebind the actions:
- "Swap Item With Offhand"  -> G : use your selected power  (Sneak + G = cycle power)
- "Drop Selected Item"      -> V : while holding the Origin Core (nether star in slot 9) opens the menu
Right-clicking the Origin Core also opens the menu. Commands: /menu, /origin, /power, /mana.

## Movement powers (dashes, launches, blinks) - config.yml -> movement
direction-mode: look   (default) FREE AIM: goes exactly where your crosshair points, any angle
direction-mode: keys   W/A/S/D/Space choose the direction; no keys held = where you look
Also: dash-strength-multiplier, blink-distance-multiplier, fall-protection, launch-no-keys-up,
and powers.cooldown-multiplier (scales every active power cooldown). Edit, then /wincors reload.

## Mana Crystal recipe (harder, edit in config.yml -> recipe)
    Diamond Block | Echo Shard      | Diamond Block
    Echo Shard    | Netherite Ingot | Echo Shard
    Diamond Block | Echo Shard      | Diamond Block
Run /wincors reload after changing it.

## Admin (/wincors, op or wincors.admin) - running /wincors in-game opens the admin GUI. 58 actions:
Info:       info, top, stats, players, ping, egg (info/give)
Mana:       mana set/add/take/reset, origin set/random/clear/swap, resetplayer
Items:      crystal, crystalall, weapon give/reset, mace info/reset/give
Combat:     combat clear/status/tag/toggle, cooldown reset <player|all>
The End:    end open/close/toggle/status/evacuate
Vanish:     /vanish [player]  or  /vanish list   (alias /v, also /wincors vanish)
Tools:      heal, feed, fly, god, gamemode, speed
Moderation: tp, tphere, freeze, mute, clearinv, invsee, enderchest, kill, smite, broadcast
Server:     config get/set, backup, save, reload, gui, help

## Vanish
Hidden from every player without wincors.admin (admins can still see each other), silent join/quit,
optional fake "left/joined the game" messages, mobs ignore you, no item pickup, no pressure plates,
saved across restarts, "VANISHED" shown in your action bar. Note: vanilla /list and the server-list
player count can still reveal vanished players.
