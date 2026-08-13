package fr.dinar;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record BalanceSyncPayload(NbtCompound data) implements CustomPayload {
    public static final CustomPayload.Id<BalanceSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of(DinarMod.MOD_ID, "balance_sync"));

    public static final PacketCodec<PacketByteBuf, BalanceSyncPayload> CODEC = PacketCodec.of(
            (payload, buf) -> buf.writeNbt(payload.data()),
            buf -> new BalanceSyncPayload(buf.readNbt())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
