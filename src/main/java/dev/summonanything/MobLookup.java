package dev.summonanything;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Finds mobs (from any mod) by typing a name, for the Magic Station's "named item" method. */
final class MobLookup {
    private MobLookup() {}

    /**
     * Matches, in this order, and returns the first non-empty group:
     * 1) the exact id (modid:mob_name)  2) the exact mob name part (mob_name)
     * 3) the exact display name (Iron Golem)  4) ids that contain the text.
     * Spaces count as underscores. Players, boats, arrows and other non-mobs are ignored.
     */
    static List<Identifier> search(String raw) {
        String text = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        String idText = text.replace(' ', '_');
        if (text.isEmpty()) return List.of();

        List<Identifier> all = new ArrayList<>();
        for (Identifier id : Registries.ENTITY_TYPE.getIds()) {
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            if (type.getSpawnGroup() != SpawnGroup.MISC) all.add(id);
        }
        all.sort(Comparator.comparing(Identifier::toString));

        List<Identifier> exactId = new ArrayList<>();
        List<Identifier> exactPath = new ArrayList<>();
        List<Identifier> exactName = new ArrayList<>();
        List<Identifier> contains = new ArrayList<>();
        for (Identifier id : all) {
            String full = id.toString();
            if (full.equals(idText)) exactId.add(id);
            if (id.getPath().equals(idText)) exactPath.add(id);
            if (Registries.ENTITY_TYPE.get(id).getName().getString().toLowerCase(Locale.ROOT).equals(text)) exactName.add(id);
            if (full.contains(idText)) contains.add(id);
        }
        if (!exactId.isEmpty()) return exactId;
        if (!exactPath.isEmpty()) return exactPath;
        if (!exactName.isEmpty()) return exactName;
        return contains;
    }
}
