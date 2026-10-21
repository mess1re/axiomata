package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionStatus;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionSync;
import net.minecraft.network.FriendlyByteBuf;
//? if neoforge {
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.BlueprintModule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

// Sent when a build comes into a player's view and whenever its progress changes.
//? if forge {
/*public record S2CConstructionStatePacket(int entityId, ConstructionStatus status) {
*///?} else {
public record S2CConstructionStatePacket(int entityId, ConstructionStatus status) implements CustomPacketPayload {
    public static final Type<S2CConstructionStatePacket> TYPE = new Type<>(
            ResourceIds.id(BlueprintModule.MOD_ID, "construction_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CConstructionStatePacket> STREAM_CODEC =
            StreamCodec.ofMember(S2CConstructionStatePacket::write, S2CConstructionStatePacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<S2CConstructionStatePacket> type() {
        return TYPE;
    }
    //?}
    private static final int MAX_ID_LENGTH = 256;

    public static void encode(S2CConstructionStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeUtf(packet.status.blueprintId(), MAX_ID_LENGTH);
        buffer.writeVarInt(packet.status.builtStages());
        buffer.writeVarInt(packet.status.hits());
        buffer.writeVarInt(packet.status.stageHits());
        buffer.writeBoolean(packet.status.complete());
    }

    public static S2CConstructionStatePacket decode(FriendlyByteBuf buffer) {
        return new S2CConstructionStatePacket(buffer.readVarInt(), new ConstructionStatus(
                buffer.readUtf(MAX_ID_LENGTH), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean()));
    }

    public static void handle(S2CConstructionStatePacket packet, NetworkManager.PacketContext context) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> context.queue(() -> {
            var level = context.getPlayer() == null ? null : context.getPlayer().level();
            var entity = level == null ? null : level.getEntity(packet.entityId);
            if (entity != null) {
                ConstructionSync.receive(entity, packet.status);
            }
        }));
    }
}
