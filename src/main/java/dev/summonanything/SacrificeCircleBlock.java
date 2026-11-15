package dev.summonanything;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

/**
 * Sacrifice Circle: a flat ritual circle. Hold a Mob Summon Book and right-click it to sacrifice the mobs
 * standing on/near the circle. Each sacrificed mob gives the book uses back (config: sacrificeUsesPerMob).
 * Named mobs, tamed pets and players are never sacrificed.
 */
public class SacrificeCircleBlock extends Block {
    private static final VoxelShape SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 0.0625, 1.0);

    public SacrificeCircleBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        for (int i = 0; i < 2; i++) {
            world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + random.nextDouble(), pos.getY() + 0.1, pos.getZ() + random.nextDouble(),
                    0.0, 0.03, 0.0);
        }
    }

    private static boolean canSacrifice(MobEntity mob) {
        if (!mob.isAlive() || mob.hasCustomName()) return false;
        return !(mob instanceof TameableEntity tameable && tameable.isTamed());
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }

        ItemStack book = player.getMainHandStack();
        if (!(book.getItem() instanceof MobBookItem)) {
            player.sendMessage(Text.literal("Hold a Mob Summon Book and stand mobs on the circle, then right-click it."), true);
            return ActionResult.CONSUME;
        }
        if (!book.isDamageable()) {
            player.sendMessage(Text.literal("This book has unlimited uses. Nothing to restore."), true);
            return ActionResult.CONSUME;
        }
        int missing = book.getDamage();
        if (missing <= 0) {
            player.sendMessage(Text.literal("This book is already full."), true);
            return ActionResult.CONSUME;
        }

        SummonAnythingConfig config = SummonAnythingConfig.get();
        int perMob = Math.max(1, config.sacrificeUsesPerMob);
        double radius = Math.max(1.0, config.sacrificeRadius);

        Box area = new Box(pos).expand(radius, 1.5, radius);
        List<MobEntity> victims = serverWorld.getEntitiesByClass(MobEntity.class, area, SacrificeCircleBlock::canSacrifice);
        if (victims.isEmpty()) {
            player.sendMessage(Text.literal("No mobs on the circle. Lure mobs within " + Math.round(radius) + " blocks of it."), true);
            return ActionResult.CONSUME;
        }
        victims.sort(Comparator.comparingDouble(m -> m.squaredDistanceTo(pos.toCenterPos())));

        int needed = (missing + perMob - 1) / perMob;
        int taken = 0;
        for (MobEntity victim : victims) {
            if (taken >= needed) break;
            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    victim.getX(), victim.getY() + victim.getHeight() / 2.0, victim.getZ(),
                    20, 0.3, 0.4, 0.3, 0.05);
            victim.kill();
            taken++;
        }

        int restored = Math.min(missing, taken * perMob);
        book.setDamage(missing - restored);
        serverWorld.playSound(null, pos, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.BLOCKS, 1.0F, 1.0F);
        player.sendMessage(Text.literal("Sacrificed " + taken + (taken == 1 ? " mob" : " mobs") + ": +" + restored
                + " uses (" + (book.getMaxDamage() - book.getDamage()) + " left)"), true);
        return ActionResult.CONSUME;
    }
}
