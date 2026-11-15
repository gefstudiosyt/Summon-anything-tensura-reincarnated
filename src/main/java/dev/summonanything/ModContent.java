package dev.summonanything;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;

/** Registers the Magic Station block and the Mob Summon Book item. */
public final class ModContent {
    public static Block MAGIC_STATION;
    public static Item MAGIC_STATION_ITEM;
    public static Block SACRIFICE_CIRCLE;
    public static Item SACRIFICE_CIRCLE_ITEM;
    public static Item MOB_BOOK;
    public static ExtendedScreenHandlerType<StationScreenHandler, BlockPos> STATION_HANDLER;

    private ModContent() {}

    public static void register() {
        Identifier stationId = Identifier.of(SummonAnything.MOD_ID, "magic_station");
        MAGIC_STATION = Registry.register(Registries.BLOCK, stationId,
                new MagicStationBlock(AbstractBlock.Settings.create()
                        .mapColor(MapColor.PURPLE)
                        .strength(3.0F, 6.0F)
                        .sounds(BlockSoundGroup.STONE)
                        .luminance(state -> 7)));
        MAGIC_STATION_ITEM = Registry.register(Registries.ITEM, stationId,
                new BlockItem(MAGIC_STATION, new Item.Settings()));
        Identifier circleId = Identifier.of(SummonAnything.MOD_ID, "sacrifice_circle");
        SACRIFICE_CIRCLE = Registry.register(Registries.BLOCK, circleId,
                new SacrificeCircleBlock(AbstractBlock.Settings.create()
                        .mapColor(MapColor.RED)
                        .strength(2.0F, 6.0F)
                        .sounds(BlockSoundGroup.STONE)
                        .luminance(state -> 10)
                        .nonOpaque()));
        SACRIFICE_CIRCLE_ITEM = Registry.register(Registries.ITEM, circleId,
                new BlockItem(SACRIFICE_CIRCLE, new Item.Settings()));

        int uses = Math.max(0, SummonAnythingConfig.get().bookUses);
        Item.Settings bookSettings = new Item.Settings();
        if (uses > 0) {
            bookSettings.maxDamage(uses);   // limited uses (a damageable item is never stackable)
        } else {
            bookSettings.maxCount(1);
        }
        MOB_BOOK = Registry.register(Registries.ITEM, Identifier.of(SummonAnything.MOD_ID, "mob_book"),
                new MobBookItem(bookSettings));

        STATION_HANDLER = Registry.register(Registries.SCREEN_HANDLER, stationId,
                new ExtendedScreenHandlerType<>(StationScreenHandler::new, BlockPos.PACKET_CODEC));

        // Own creative tab, so the blocks are easy to find (and show up in JEI).
        Registry.register(Registries.ITEM_GROUP, Identifier.of(SummonAnything.MOD_ID, "main"),
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(MAGIC_STATION_ITEM))
                        .displayName(Text.translatable("itemGroup.summonanything.main"))
                        .entries((context, entries) -> {
                            entries.add(MAGIC_STATION_ITEM);
                            entries.add(SACRIFICE_CIRCLE_ITEM);
                            entries.add(MOB_BOOK);
                        })
                        .build());

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(MAGIC_STATION_ITEM);
            entries.add(SACRIFICE_CIRCLE_ITEM);
        });
    }
}
