package dev.summonanything;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class SummonAnything implements ModInitializer {
    public static final String MOD_ID = "summonanything";

    @Override
    public void onInitialize() {
        // Creates config/summonanything.json on first launch so it is ready to edit.
        SummonAnythingConfig.get();
        ModContent.register();

        PayloadTypeRegistry.playC2S().register(SelectMobPayload.ID, SelectMobPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SelectMobPayload.ID, (payload, context) -> {
            if (context.player().currentScreenHandler instanceof StationScreenHandler handler) {
                handler.createBook(context.player(), payload.mob());
            }
        });
    }
}
