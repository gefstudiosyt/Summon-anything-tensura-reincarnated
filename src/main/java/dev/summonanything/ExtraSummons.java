package dev.summonanything;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.UUID;

/** Does the actual summoning for mobs listed in config/summonanything.json. */
public final class ExtraSummons {
    private static final Logger LOGGER = LoggerFactory.getLogger("SummonAnything");
    private static boolean warnedTensuraOwner = false;

    private ExtraSummons() {}

    /**
     * Mirrors what Tensura's SummonOtherworlderMagic.createSummon does, but for any mob.
     *
     * @return true if a configured mob was summoned (the original Tensura code must then be skipped)
     */
    public static boolean trySummon(Object skillInstance, LivingEntity summoner, Vec3d pos, String uuidKey) {
        Entity entity = null;
        try {
            SummonAnythingConfig config = SummonAnythingConfig.get();
            if (config.mobs == null || config.mobs.isEmpty()) return false;
            if (summoner.getRandom().nextDouble() >= config.extraMobChance) return false;
            if (!(summoner.getWorld() instanceof ServerWorld world)) return false;

            EntityType<?> type = config.pickMob(summoner.getRandom());
            if (type == null) return false;

            entity = type.create(world);
            if (!(entity instanceof MobEntity mob)) {
                // Only mobs can be "called forth" by the spell; fall back to normal Tensura behaviour.
                if (entity != null) entity.discard();
                return false;
            }

            // Same steps as Tensura: start hidden under the magic circle, no AI, no collision.
            mob.setAiDisabled(true);
            mob.noClip = true;
            mob.setPosition(pos.add(0.0, -1.5 * mob.getHeight(), 0.0));
            mob.initialize(world, world.getLocalDifficulty(BlockPos.ofFloored(pos)), SpawnReason.EVENT, null);

            setTensuraTemporaryOwner(mob, summoner.getUuid());

            world.spawnEntity(mob);
            mob.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, summoner.getEyePos());

            // Tensura remembers the summoned mob by UUID inside the skill's data.
            Object tag = skillInstance.getClass().getMethod("getOrCreateTag").invoke(skillInstance);
            if (tag instanceof NbtCompound compound) {
                compound.putUuid(uuidKey, mob.getUuid());
            }
            return true;
        } catch (Throwable t) {
            LOGGER.error("Summoning a configured mob failed; falling back to normal Tensura summon.", t);
            if (entity != null && !entity.isRemoved()) entity.discard();
            return false;
        }
    }

    /**
     * Spends magicule from a player using Tensura's storage.
     *
     * @return false only when the player definitely has too little; true if it was spent, if cost is 0,
     *         or if Tensura's storage could not be reached (then no cost is charged).
     */
    static boolean trySpendMagicule(LivingEntity entity, double cost) {
        if (cost <= 0.0) return true;
        try {
            Class<?> storages = Class.forName("io.github.manasmods.tensura.storage.TensuraStorages");
            Class<?> existence = Class.forName("io.github.manasmods.tensura.storage.ep.IExistence");
            Object instance = storages.getMethod("getExistenceFrom", LivingEntity.class).invoke(null, entity);
            double current = ((Number) existence.getMethod("getMagicule").invoke(instance)).doubleValue();
            if (current < cost) return false;
            existence.getMethod("setMagicule", double.class).invoke(instance, current - cost);
            existence.getMethod("markDirty").invoke(instance);
            return true;
        } catch (Throwable t) {
            LOGGER.warn("Could not charge magicule (book used for free): {}", t.toString());
            return true;
        }
    }

    /** Best effort: marks the summoner as owner the same way Tensura does for its own summons. */
    static void setTensuraTemporaryOwner(MobEntity mob, UUID owner) {
        try {
            Class<?> storages = Class.forName("io.github.manasmods.tensura.storage.TensuraStorages");
            Class<?> existence = Class.forName("io.github.manasmods.tensura.storage.ep.IExistence");
            Object instance = storages.getMethod("getExistenceFrom", LivingEntity.class).invoke(null, mob);
            Method setOwner = existence.getMethod("setTemporaryOwner", UUID.class);
            setOwner.invoke(instance, owner);
            existence.getMethod("markDirty").invoke(instance);
        } catch (Throwable t) {
            if (!warnedTensuraOwner) {
                warnedTensuraOwner = true;
                LOGGER.warn("Could not set Tensura temporary owner (summon still works): {}", t.toString());
            }
        }
    }
}
