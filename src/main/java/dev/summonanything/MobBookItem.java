package dev.summonanything;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Right-click to summon the mob this book was made for. Made at the Magic Station.
 * Costs magicule (bookMagiculeCost) and has a limited number of uses (bookUses), both in the config.
 */
public class MobBookItem extends Item {
    public static final String KEY = "summonanything_mob";

    public MobBookItem(Settings settings) {
        super(settings);
    }

    public static Identifier getMobId(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component == null) return null;
        NbtCompound tag = component.copyNbt();
        return tag.contains(KEY) ? Identifier.tryParse(tag.getString(KEY)) : null;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.success(stack);
        }

        Identifier id = getMobId(stack);
        if (id != null && SummonAnythingConfig.isBlocked(id)) {
            player.sendMessage(Text.literal("That mob cannot be summoned (iron and snow golems must be obtained normally)."), true);
            return TypedActionResult.fail(stack);
        }
        if (id == null || !Registries.ENTITY_TYPE.containsId(id)) {
            player.sendMessage(Text.literal("This book is empty or its mob is not installed. Make one at a Magic Station."), true);
            return TypedActionResult.fail(stack);
        }

        EntityType<?> type = Registries.ENTITY_TYPE.get(id);
        Entity entity = type.create(serverWorld);
        if (!(entity instanceof MobEntity mob)) {
            if (entity != null) entity.discard();
            player.sendMessage(Text.literal("That is not a mob that can be summoned."), true);
            return TypedActionResult.fail(stack);
        }

        // Magicule cost (Tensura). Creative players pay nothing.
        double cost = SummonAnythingConfig.get().bookMagiculeCost;
        if (!player.isCreative() && !ExtraSummons.trySpendMagicule(player, cost)) {
            entity.discard();
            player.sendMessage(Text.literal("Not enough magicule (needs " + Math.round(cost) + ")."), true);
            return TypedActionResult.fail(stack);
        }

        Vec3d look = player.getRotationVec(1.0F);
        Vec3d spot = player.getPos().add(look.x * 2.0, 0.0, look.z * 2.0);
        mob.refreshPositionAndAngles(spot.x, spot.y, spot.z, player.getYaw() + 180.0F, 0.0F);
        mob.initialize(serverWorld, serverWorld.getLocalDifficulty(mob.getBlockPos()), SpawnReason.MOB_SUMMONED, null);
        ExtraSummons.setTensuraTemporaryOwner(mob, player.getUuid());
        serverWorld.spawnEntity(mob);

        // One use is spent. The book breaks when its uses run out (if bookUses is above 0).
        if (stack.isDamageable()) {
            stack.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        int cooldownTicks = Math.max(0, SummonAnythingConfig.get().bookCooldownSeconds) * 20;
        if (cooldownTicks > 0) {
            player.getItemCooldownManager().set(this, cooldownTicks);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        Identifier id = getMobId(stack);
        if (id != null && Registries.ENTITY_TYPE.containsId(id)) {
            tooltip.add(Text.literal("Summons: ").append(Registries.ENTITY_TYPE.get(id).getName()).formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.literal("Empty - make one at a Magic Station").formatted(Formatting.GRAY));
        }
        double cost = SummonAnythingConfig.get().bookMagiculeCost;
        if (cost > 0) {
            tooltip.add(Text.literal("Cost: " + Math.round(cost) + " magicule").formatted(Formatting.DARK_AQUA));
        }
        if (stack.isDamageable()) {
            tooltip.add(Text.literal("Uses left: " + (stack.getMaxDamage() - stack.getDamage())).formatted(Formatting.DARK_AQUA));
        }
    }
}
