package dev.summonanything;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Slot 0 = Summon Otherworlder tome (input), slot 1 = Mob Summon Book (output, take only). */
public class StationScreenHandler extends ScreenHandler {
    private final Inventory inventory = new SimpleInventory(2);
    private final BlockPos pos;

    public StationScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        super(ModContent.STATION_HANDLER, syncId);
        this.pos = pos;

        this.addSlot(new Slot(inventory, 0, 142, 36) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return TomeCheck.isSummonTome(stack);
            }
        });
        this.addSlot(new Slot(inventory, 1, 142, 88) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    /** Called on the server when the player presses Create. */
    public void createBook(ServerPlayerEntity player, Identifier mobId) {
        ItemStack tome = inventory.getStack(0);
        if (tome.isEmpty() || !TomeCheck.isSummonTome(tome)) {
            player.sendMessage(Text.literal("Put a Summon Otherworlder spell book in the slot."), true);
            return;
        }
        if (!inventory.getStack(1).isEmpty()) {
            player.sendMessage(Text.literal("Take the finished book out first."), true);
            return;
        }
        if (mobId == null || !Registries.ENTITY_TYPE.containsId(mobId) || SummonAnythingConfig.isBlocked(mobId)) {
            player.sendMessage(Text.literal("That mob cannot be summoned (iron and snow golems must be obtained normally)."), true);
            return;
        }
        EntityType<?> type = Registries.ENTITY_TYPE.get(mobId);
        Entity probe = type.create(player.getServerWorld());
        boolean isMob = probe instanceof MobEntity;
        if (probe != null) probe.discard();
        if (!isMob) {
            player.sendMessage(Text.literal("That is not a mob that can be summoned."), true);
            return;
        }

        ItemStack book = new ItemStack(ModContent.MOB_BOOK);
        NbtCompound tag = new NbtCompound();
        tag.putString(MobBookItem.KEY, mobId.toString());
        book.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));

        if (!player.isCreative()) {
            tome.decrement(1);
            if (tome.isEmpty()) inventory.setStack(0, ItemStack.EMPTY);
        }
        inventory.setStack(1, book);
        this.sendContentUpdates();
        player.getServerWorld().playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        player.sendMessage(Text.literal("Created a Mob Summon Book for " + mobId), true);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < 2) {
                if (!this.insertItem(stack, 2, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (TomeCheck.isSummonTome(stack)) {
                if (!this.insertItem(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getWorld().getBlockState(pos).isOf(ModContent.MAGIC_STATION)
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.dropInventory(player, inventory);
    }
}
