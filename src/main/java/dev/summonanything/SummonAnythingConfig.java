package dev.summonanything;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * config/summonanything.json
 * Re-read automatically whenever the file changes, so you can edit it while the game runs.
 */
public final class SummonAnythingConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("SummonAnything");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("summonanything.json");

    private static SummonAnythingConfig current = new SummonAnythingConfig();
    private static long lastModified = -1L;
    private static boolean loadedOnce = false;

    public String _help1 = "Each cast of Summon Otherworlder has 'extraMobChance' (0.0 to 1.0) of summoning one of the mobs below instead of a normal Tensura otherworlder.";
    public String _help2 = "Add a mob: copy a line in 'mobs', set 'id' to the mob's ID (example: modid:mob_name) and 'weight' to how likely it is compared to the others.";
    public String _help3 = "Find IDs with F3+H and hold a spawn egg, or type /summon and press Tab. Wrong IDs are skipped. Hostile mobs will be hostile to you.";
    public double extraMobChance = 0.5;
    public String _help4 = "stationMobs: mobs you can pick at the Magic Station WITHOUT a spawn egg (empty off hand). Add any mob id from any mod. Mob Summon Book: bookCooldownSeconds, bookMagiculeCost (0 = free), bookUses (0 = unlimited; needs a game restart to change). Sacrifice Circle: sacrificeRadius (blocks) and sacrificeUsesPerMob (uses a sacrificed mob gives back).";
    public int bookCooldownSeconds = 60;
    public double bookMagiculeCost = 500.0;
    public int bookUses = 10;
    public double sacrificeRadius = 3.0;
    public int sacrificeUsesPerMob = 1;
    public List<String> stationMobs = new ArrayList<>();
    public String _help5 = "blockedMobs: mobs that can never be summoned by this mod. Iron golems and snow golems are ALWAYS blocked (you have to get them normally), you cannot unblock them here.";
    public List<String> blockedMobs = new ArrayList<>();
    public List<Entry> mobs = defaultMobs();

    public static final class Entry {
        public String id;
        public double weight = 1.0;

        public Entry() {}

        public Entry(String id, double weight) {
            this.id = id;
            this.weight = weight;
        }
    }

    private static List<Entry> defaultMobs() {
        List<Entry> list = new ArrayList<>();
        list.add(new Entry("minecraft:villager", 1.0));
        list.add(new Entry("minecraft:wandering_trader", 1.0));
        return list;
    }

    /** Returns the current config, re-reading the file if it changed. */
    public static synchronized SummonAnythingConfig get() {
        try {
            if (!Files.exists(FILE)) {
                SummonAnythingConfig fresh = new SummonAnythingConfig();
                try (Writer w = Files.newBufferedWriter(FILE)) {
                    GSON.toJson(fresh, w);
                }
                current = fresh;
                lastModified = Files.getLastModifiedTime(FILE).toMillis();
                loadedOnce = true;
                LOGGER.info("Created {}", FILE);
                return current;
            }
            long modified = Files.getLastModifiedTime(FILE).toMillis();
            if (!loadedOnce || modified != lastModified) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    SummonAnythingConfig loaded = GSON.fromJson(r, SummonAnythingConfig.class);
                    if (loaded != null && loaded.mobs != null) {
                        current = loaded;
                        LOGGER.info("Loaded {} ({} mob entries)", FILE.getFileName(), loaded.mobs.size());
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not read {} - keeping the previous settings. Check for a typo (missing comma or quote).", FILE, e);
                }
                lastModified = modified;
                loadedOnce = true;
            }
        } catch (IOException e) {
            LOGGER.error("Config file problem", e);
        }
        return current;
    }

    /** Writes the current settings to disk (used by the in-game menu). */
    public static synchronized void save() {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(current, w);
        } catch (IOException e) {
            LOGGER.error("Could not save {}", FILE, e);
            return;
        }
        try {
            lastModified = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException ignored) {
            // Worst case the file is simply re-read on the next cast.
        }
        loadedOnce = true;
    }

    private static final Set<String> ALWAYS_BLOCKED = Set.of("minecraft:iron_golem", "minecraft:snow_golem");

    /** True if this mob must never be summoned by the mod (golems, plus 'blockedMobs' from the config). */
    public static boolean isBlocked(Identifier id) {
        if (id == null) return true;
        String text = id.toString();
        if (ALWAYS_BLOCKED.contains(text)) return true;
        List<String> extra = get().blockedMobs;
        if (extra != null) {
            for (String b : extra) {
                if (b != null && b.trim().equals(text)) return true;
            }
        }
        return false;
    }

    /** True for the always-blocked golems only (safe to call on a client, no config needed). */
    public static boolean isAlwaysBlocked(Identifier id) {
        return id != null && ALWAYS_BLOCKED.contains(id.toString());
    }

    /** The mobs from 'stationMobs' that exist in this game, in order. */
    public List<Identifier> stationMobChoices() {
        List<Identifier> result = new ArrayList<>();
        if (stationMobs == null) return result;
        for (String text : stationMobs) {
            if (text == null) continue;
            Identifier id = Identifier.tryParse(text.trim());
            if (id != null && Registries.ENTITY_TYPE.containsId(id) && !isBlocked(id)) {
                result.add(id);
            } else {
                LOGGER.warn("Skipping unknown stationMobs entry '{}'", text);
            }
        }
        return result;
    }

    /** Returns the entry for a mob id, or null if it is not in the list. */
    public Entry find(String id) {
        for (Entry e : mobs) {
            if (e != null && id.equals(e.id)) return e;
        }
        return null;
    }

    /** Picks a random registered mob type using the weights, or null if none are usable. */
    public EntityType<?> pickMob(Random random) {
        List<EntityType<?>> types = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0.0;
        for (Entry e : mobs) {
            if (e == null || e.id == null || e.weight <= 0.0) continue;
            Identifier id = Identifier.tryParse(e.id.trim());
            if (id == null) {
                LOGGER.warn("Skipping invalid mob id '{}'", e.id);
                continue;
            }
            if (isBlocked(id)) continue;
            var type = Registries.ENTITY_TYPE.getOrEmpty(id);
            if (type.isEmpty()) {
                LOGGER.warn("Skipping unknown mob id '{}' (is that mod installed?)", e.id);
                continue;
            }
            types.add(type.get());
            weights.add(e.weight);
            total += e.weight;
        }
        if (types.isEmpty()) return null;
        double roll = random.nextDouble() * total;
        for (int i = 0; i < types.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0.0) return types.get(i);
        }
        return types.get(types.size() - 1);
    }
}
