package dev.summonanything;

import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Recognises Tensura's Summon Otherworlder spell book (a Magic Tome holding that skill). */
public final class TomeCheck {
    private TomeCheck() {}

    public static boolean isSummonTome(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (!itemId.getNamespace().equals("tensura")) return false;
        try {
            ComponentType<?> skill = Registries.DATA_COMPONENT_TYPE.get(Identifier.of("tensura", "skill"));
            if (skill != null) {
                Object value = stack.get(skill);
                if (value != null) {
                    return value.toString().toLowerCase().contains("summon_otherworlder");
                }
            }
        } catch (Throwable ignored) {
            // fall through to the name check
        }
        // Fallback: the tome's display name mentions the spell.
        return stack.getName().getString().toLowerCase().contains("summon otherworlder");
    }
}
