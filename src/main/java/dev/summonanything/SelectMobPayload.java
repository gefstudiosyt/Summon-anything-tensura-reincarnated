package dev.summonanything;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: "make a Mob Summon Book for this mob" from the Magic Station screen. */
public record SelectMobPayload(Identifier mob) implements CustomPayload {
    public static final CustomPayload.Id<SelectMobPayload> ID =
            new CustomPayload.Id<>(Identifier.of(SummonAnything.MOD_ID, "select_mob"));
    public static final PacketCodec<RegistryByteBuf, SelectMobPayload> CODEC =
            PacketCodec.tuple(Identifier.PACKET_CODEC, SelectMobPayload::mob, SelectMobPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
