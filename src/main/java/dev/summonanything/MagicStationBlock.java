package dev.summonanything;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Magic Station. Right-click to open its screen: put a Summon Otherworlder spell book in the slot,
 * pick a mob from the list (any mod), press Create and take the Mob Summon Book.
 */
public class MagicStationBlock extends Block {
    public MagicStationBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.openHandledScreen(new ExtendedScreenHandlerFactory<BlockPos>() {
                @Override
                public BlockPos getScreenOpeningData(ServerPlayerEntity p) {
                    return pos;
                }

                @Override
                public Text getDisplayName() {
                    return Text.translatable("block.summonanything.magic_station");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity p) {
                    return new StationScreenHandler(syncId, inventory, pos);
                }
            });
        }
        return ActionResult.CONSUME;
    }
}
