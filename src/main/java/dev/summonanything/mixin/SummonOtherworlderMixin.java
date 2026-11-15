package dev.summonanything.mixin;

import dev.summonanything.ExtraSummons;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets Tensura's Summon Otherworlder spell by name, so this mod does not need Tensura to compile.
 * Tensura's own classes are not obfuscated, but Minecraft types in method descriptors use
 * intermediary names at runtime (class_1309 = LivingEntity, class_1308 = MobEntity, class_243 = Vec3d).
 */
@Pseudo
@Mixin(targets = "io.github.manasmods.tensura.ability.magic.summon.SummonOtherworlderMagic")
public class SummonOtherworlderMixin {

    /** Replaces Tensura's spawn step when a configured mob is rolled; otherwise Tensura runs normally. */
    @Inject(method = "createSummon", at = @At("HEAD"), cancellable = true, remap = false)
    private void summonanything$createSummon(@Coerce Object skillInstance, LivingEntity summoner, int mode,
                                             Vec3d pos, String uuidKey, CallbackInfo ci) {
        if (ExtraSummons.trySummon(skillInstance, summoner, pos, uuidKey)) {
            ci.cancel();
        }
    }

    /**
     * Tensura's generic bridge method casts every summoned mob to PassiveEntity (AgeableMob) and
     * crashes for anything else. Mobs that are not passive types skip that check here.
     */
    @Inject(
        method = "isSummoningRequirementFulfilled(Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;Lnet/minecraft/class_1309;Lnet/minecraft/class_1308;I)Z",
        at = @At("HEAD"), cancellable = true, remap = false)
    private void summonanything$requirement(@Coerce Object skillInstance, LivingEntity summoner, MobEntity entity,
                                            int mode, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof PassiveEntity)) {
            cir.setReturnValue(true);
        }
    }
}
