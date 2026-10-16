package com.wincors.smp;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Pretty particle animations: coloured gradient glows, rotating magic circles, double helixes,
 * glowing shells, shockwaves, beams and trails. The colours come from the power's particle.
 */
final class Fx {
    private static final Map<Particle, Color[]> PALETTES = new HashMap<>();

    private Fx() {}

    // ---------------- settings ----------------

    static boolean enabled() {
        WincorsSMP pl = Abilities.plugin();
        return pl == null || pl.getConfig().getBoolean("visuals.enabled", true);
    }

    private static boolean dustOn() {
        WincorsSMP pl = Abilities.plugin();
        return pl == null || pl.getConfig().getBoolean("visuals.colored-dust", true);
    }

    private static boolean circlesOn() {
        WincorsSMP pl = Abilities.plugin();
        return pl == null || pl.getConfig().getBoolean("visuals.magic-circles", true);
    }

    static int n(int base) {
        WincorsSMP pl = Abilities.plugin();
        double d = pl == null ? 1.0 : Math.max(0.2, Math.min(3.0, pl.getConfig().getDouble("visuals.density", 1.0)));
        return Math.max(1, (int) Math.round(base * d));
    }

    // ---------------- colours ----------------

    private static Color rgb(int r, int g, int b) { return Color.fromRGB(r, g, b); }

    /** Two colours (start -> end) for a particle theme. */
    static Color[] palette(Particle p) {
        return PALETTES.computeIfAbsent(p, k -> switch (k.getKey().getKey()) {
            case "flame", "small_flame", "lava" -> new Color[]{rgb(255, 80, 20), rgb(255, 205, 60)};
            case "splash", "dripping_water", "bubble_pop" -> new Color[]{rgb(30, 110, 255), rgb(110, 230, 255)};
            case "crit", "enchanted_hit" -> new Color[]{rgb(125, 85, 50), rgb(90, 190, 90)};
            case "cloud" -> new Color[]{rgb(235, 240, 255), rgb(150, 200, 255)};
            case "smoke", "large_smoke" -> new Color[]{rgb(70, 40, 110), rgb(170, 100, 230)};
            case "electric_spark" -> new Color[]{rgb(255, 240, 80), rgb(255, 255, 255)};
            case "snowflake" -> new Color[]{rgb(110, 215, 255), rgb(255, 255, 255)};
            case "happy_villager" -> new Color[]{rgb(50, 200, 90), rgb(190, 255, 120)};
            case "angry_villager" -> new Color[]{rgb(200, 20, 20), rgb(255, 110, 60)};
            case "witch", "portal" -> new Color[]{rgb(150, 60, 255), rgb(255, 110, 230)};
            case "end_rod" -> new Color[]{rgb(255, 255, 255), rgb(200, 220, 255)};
            case "soul_fire_flame" -> new Color[]{rgb(60, 180, 255), rgb(120, 255, 230)};
            case "explosion" -> new Color[]{rgb(255, 140, 40), rgb(255, 230, 120)};
            case "heart" -> new Color[]{rgb(255, 90, 140), rgb(255, 170, 200)};
            default -> new Color[]{rgb(255, 200, 60), rgb(255, 255, 255)};
        });
    }

    private static Color lerp(Color a, Color b, double f) {
        f = Math.max(0, Math.min(1, f));
        return Color.fromRGB((int) (a.getRed() + (b.getRed() - a.getRed()) * f),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * f),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * f));
    }

    // ---------------- primitives ----------------

    static void dot(World w, Particle p, double x, double y, double z) {
        try { w.spawnParticle(p, x, y, z, 1, 0, 0, 0, 0); } catch (Exception ignored) { }
    }

    /** One glowing point that fades from the theme's first colour to its second. */
    static void glow(World w, Particle theme, float size, double x, double y, double z) {
        if (!dustOn()) { dot(w, theme, x, y, z); return; }
        Color[] pal = palette(theme);
        try {
            w.spawnParticle(Particle.DUST_COLOR_TRANSITION, x, y, z, 1, 0, 0, 0, 0, new Particle.DustTransition(pal[0], pal[1], size));
        } catch (Exception ignored) { }
    }

    /** One glowing point at position f (0..1) of the theme's colour gradient. */
    static void glowAt(World w, Particle theme, float size, double f, double x, double y, double z) {
        if (!dustOn()) { dot(w, theme, x, y, z); return; }
        Color[] pal = palette(theme);
        try {
            w.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, new Particle.DustOptions(lerp(pal[0], pal[1], f), size));
        } catch (Exception ignored) { }
    }

    static void burst(Location l, Particle p, int count, double dx, double dy, double dz, double speed) {
        if (!enabled()) return;
        try { l.getWorld().spawnParticle(p, l, n(count), dx, dy, dz, speed); } catch (Exception ignored) { }
    }

    private static void run(int ticks, IntConsumer step) {
        WincorsSMP pl = Abilities.plugin();
        if (pl == null) return;
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                try {
                    step.accept(t);
                } catch (Exception ex) {
                    cancel();
                    return;
                }
                if (++t >= ticks) cancel();
            }
        }.runTaskTimer(pl, 0L, 1L);
    }

    private static double ease(double x) { return 1 - (1 - x) * (1 - x); }

    private static void ringGlow(Location c, double r, Particle p, double yOff, float size, int pts) {
        World w = c.getWorld();
        for (int i = 0; i < pts; i++) {
            double a = Math.PI * 2 * i / pts;
            glow(w, p, size, c.getX() + Math.cos(a) * r, c.getY() + yOff, c.getZ() + Math.sin(a) * r);
        }
    }

    // ---------------- effects ----------------

    /** A single flat glowing ring. */
    static void ring(Location c, double r, Particle p, double yOff) {
        if (!enabled()) return;
        ringGlow(c, r, p, yOff, 1.2f, n((int) Math.max(16, r * 12)));
    }

    /** Expanding glowing ring with a trailing inner ring, radial spokes and sparkles. */
    static void shockwave(Location center, double maxR, Particle p, int ticks, int spokes) {
        if (!enabled()) return;
        Location c = center.clone();
        World w = c.getWorld();
        run(ticks, t -> {
            double prev = maxR * ease((double) t / ticks);
            double cur = maxR * ease((t + 1.0) / ticks);
            float size = (float) (1.5 - 0.6 * (t / (double) ticks));
            ringGlow(c, cur, p, 0.15, size, n((int) Math.max(18, cur * 12)));
            if (cur > 1.2) ringGlow(c, cur * 0.72, p, 0.15, size * 0.8f, n((int) Math.max(12, cur * 8)));
            for (int i = 0; i < n(3); i++) {
                double a = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
                dot(w, p, c.getX() + Math.cos(a) * cur, c.getY() + 0.25, c.getZ() + Math.sin(a) * cur);
            }
            for (int k = 0; k < spokes; k++) {
                double a = Math.PI * 2 * k / spokes;
                for (double d = Math.max(0.4, prev); d <= cur; d += 0.45) {
                    glow(w, p, size * 0.8f, c.getX() + Math.cos(a) * d, c.getY() + 0.15, c.getZ() + Math.sin(a) * d);
                }
                if (t == ticks - 1) dot(w, Particle.END_ROD, c.getX() + Math.cos(a) * cur, c.getY() + 0.3, c.getZ() + Math.sin(a) * cur);
            }
        });
    }

    /** Rotating magic circle with a hexagram on the ground. */
    static void magicCircle(Location center, double r, Particle p, int ticks) {
        if (!enabled() || !circlesOn()) return;
        Location c = center.clone().add(0, 0.12, 0);
        World w = c.getWorld();
        run(ticks, t -> {
            double prog = t / (double) ticks;
            float size = (float) (1.1 - 0.5 * prog);
            double rot = t * 0.10;
            ringGlow(c, r, p, 0, size, n((int) Math.max(24, r * 14)));
            ringGlow(c, r * 0.8, p, 0, size * 0.8f, n((int) Math.max(16, r * 9)));
            int steps = n(Math.max(6, (int) (r * 3)));
            for (int tri = 0; tri < 2; tri++) {
                double base = rot + tri * Math.PI / 3;
                for (int e = 0; e < 3; e++) {
                    double a1 = base + e * 2 * Math.PI / 3;
                    double a2 = base + (e + 1) * 2 * Math.PI / 3;
                    double x1 = c.getX() + Math.cos(a1) * r * 0.8, z1 = c.getZ() + Math.sin(a1) * r * 0.8;
                    double x2 = c.getX() + Math.cos(a2) * r * 0.8, z2 = c.getZ() + Math.sin(a2) * r * 0.8;
                    for (int s = 0; s < steps; s++) {
                        double f = s / (double) steps;
                        glow(w, p, size * 0.8f, x1 + (x2 - x1) * f, c.getY(), z1 + (z2 - z1) * f);
                    }
                }
            }
            if (t % 4 == 0) dot(w, Particle.END_ROD, c.getX(), c.getY() + 0.2, c.getZ());
        });
    }

    /** Shimmering shell that expands around a point (buffs). */
    static void sphere(Supplier<Location> base, double r, Particle p, int ticks) {
        if (!enabled()) return;
        run(ticks, t -> {
            Location b = base.get();
            if (b == null || b.getWorld() == null) return;
            World w = b.getWorld();
            double prog = (t + 1.0) / ticks;
            double rr = r * (0.6 + 0.4 * ease(prog));
            float size = (float) (1.0 - 0.5 * prog);
            int pts = n(34);
            double golden = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < pts; i++) {
                double y = 1 - (i / (double) (pts - 1)) * 2;
                double rad = Math.sqrt(1 - y * y);
                double th = golden * i + t * 0.2;
                glow(w, p, size, b.getX() + Math.cos(th) * rad * rr, b.getY() + y * rr, b.getZ() + Math.sin(th) * rad * rr);
            }
        });
    }

    /** Ring that closes in toward the centre (pulls). */
    static void converge(Location center, double startR, Particle p, int ticks) {
        if (!enabled()) return;
        Location c = center.clone();
        run(ticks, t -> {
            double r = Math.max(0.4, startR * (1 - ease((t + 1.0) / ticks)));
            ringGlow(c, r, p, 0.2, 1.1f, n((int) Math.max(14, r * 10)));
        });
    }

    /** Rising double helix that follows a location (pass () -> player.getLocation() to follow a player). */
    static void spiral(Supplier<Location> base, Particle p, double radius, double height, int ticks, int arms) {
        if (!enabled()) return;
        run(ticks, t -> {
            Location b = base.get();
            if (b == null || b.getWorld() == null) return;
            World w = b.getWorld();
            double prog = (t + 1.0) / ticks;
            for (int a = 0; a < arms; a++) {
                double ang = t * 0.55 + a * Math.PI * 2 / arms;
                for (int s = 0; s < 4; s++) {
                    double pr = Math.max(0, prog - s * 0.05);
                    double an = ang - s * 0.3;
                    double x = b.getX() + Math.cos(an) * radius;
                    double y = b.getY() + pr * height;
                    double z = b.getZ() + Math.sin(an) * radius;
                    glowAt(w, p, (float) (1.3 - 0.2 * s), pr, x, y, z);
                    if (s == 0 && t % 2 == 0) dot(w, Particle.END_ROD, x, y, z);
                }
            }
        });
    }

    /** Twisting double-helix column. */
    static void column(Location center, Particle p, double height, int ticks) {
        if (!enabled()) return;
        Location c = center.clone();
        World w = c.getWorld();
        run(ticks, t -> {
            for (double y = 0; y <= height; y += 0.35) {
                double a = y * 3 + t * 0.6;
                for (int side = 0; side < 2; side++) {
                    double an = a + side * Math.PI;
                    glowAt(w, p, 1.1f, y / height, c.getX() + Math.cos(an) * 0.28, c.getY() + y, c.getZ() + Math.sin(an) * 0.28);
                }
            }
        });
    }

    /** Glowing beam with a spiralling halo, shot from a to b over a few ticks. */
    static void beam(Location a, Location b, Particle p, int ticks) {
        if (!enabled()) return;
        Vector v = b.toVector().subtract(a.toVector());
        double len = v.length();
        if (len < 0.1) return;
        Vector dir = v.normalize();
        Vector u = dir.clone().crossProduct(new Vector(0, 1, 0));
        if (u.lengthSquared() < 1e-4) u = new Vector(1, 0, 0);
        u.normalize();
        Vector u2 = u;
        Vector v2 = dir.clone().crossProduct(u).normalize();
        Location start = a.clone();
        World w = start.getWorld();
        Location end = b.clone();
        run(ticks, t -> {
            double s = len * t / ticks;
            double e = len * (t + 1.0) / ticks;
            for (double d = s; d <= e; d += 0.25) {
                double ang = d * 7;
                double ox = (u2.getX() * Math.cos(ang) + v2.getX() * Math.sin(ang)) * 0.14;
                double oy = (u2.getY() * Math.cos(ang) + v2.getY() * Math.sin(ang)) * 0.14;
                double oz = (u2.getZ() * Math.cos(ang) + v2.getZ() * Math.sin(ang)) * 0.14;
                double x = start.getX() + dir.getX() * d, y = start.getY() + dir.getY() * d, z = start.getZ() + dir.getZ() * d;
                glowAt(w, p, 1.0f, d / len, x + ox, y + oy, z + oz);
                if (((int) (d * 4)) % 2 == 0) dot(w, Particle.END_ROD, x, y, z);
            }
            if (t == ticks - 1) burst(end, Particle.END_ROD, 12, 0.2, 0.2, 0.2, 0.08);
        });
    }

    /** Instant glowing line (teleport streak). */
    static void line(Location a, Location b, Particle p) {
        if (!enabled()) return;
        Vector v = b.toVector().subtract(a.toVector());
        double len = v.length();
        if (len < 0.1) return;
        Vector dir = v.normalize();
        World w = a.getWorld();
        double step = Math.max(0.35, len / 40.0);
        for (double d = 0; d <= len; d += step) {
            double x = a.getX() + dir.getX() * d, y = a.getY() + dir.getY() * d, z = a.getZ() + dir.getZ() * d;
            glowAt(w, p, 0.9f, d / len, x, y, z);
            dot(w, Particle.END_ROD, x, y, z);
        }
    }

    /** Ribbon of two orbiting glows that follows a moving player (dash / launch). */
    static void trail(Player pl, Particle p, int ticks) {
        if (!enabled()) return;
        run(ticks, t -> {
            if (!pl.isOnline()) return;
            Location l = pl.getLocation();
            World w = l.getWorld();
            for (int k = 0; k < 2; k++) {
                double a = t * 0.9 + k * Math.PI;
                glow(w, p, 1.2f, l.getX() + Math.cos(a) * 0.5, l.getY() + 0.9 + Math.sin(t * 0.5) * 0.4, l.getZ() + Math.sin(a) * 0.5);
            }
            try { w.spawnParticle(p, l.clone().add(0, 0.9, 0), n(2), 0.2, 0.3, 0.2, 0.01); } catch (Exception ignored) { }
        });
    }

    /** Hit effect: burst, small shockwave and an upward sparkle fountain. */
    static void impact(Location l, Particle p) {
        if (!enabled()) return;
        burst(l, p, 12, 0.3, 0.4, 0.3, 0.05);
        shockwave(l.clone().subtract(0, 0.9, 0), 1.8, p, 5, 4);
        burst(l, Particle.END_ROD, 10, 0.2, 0.3, 0.2, 0.1);
    }
}
